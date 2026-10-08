package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.domain.AccessDevice;
import com.ruoyi.studyroom.domain.AccessProfile;
import com.ruoyi.studyroom.mapper.AccessDeviceMapper;
import com.ruoyi.studyroom.mapper.AccessProfileMapper;
import com.ruoyi.studyroom.service.IDeviceChannel;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 门禁设备人员管理(后台)。
 *
 * <p>设备(门禁机)里存着一份人员名单, 与我们数据库里的档案不是一回事:
 * 实测设备上有 127 人, 而我们库里只有 4 条档案 —— 历史上有一批人是通过
 * 厂商页面/App 直接录进设备的, 这些人的门禁权限我们系统看不见也管不到。</p>
 *
 * <h3>为什么有这份"镜像"</h3>
 * <p>最初的做法是每次打开页面都去设备读一遍(read_user + read_face ≈ 700KB MQTT 报文),
 * 只在内存缓存 60 秒 —— 等于<b>看一次就要抓一次门禁</b>, 既慢又在打扰设备。
 * 现在改成<b>本地镜像</b>:</p>
 * <ul>
 *   <li><b>看页面</b>: 只读镜像(Redis), <b>零设备流量</b>, 秒开;</li>
 *   <li><b>录入/删除人员时</b>: 顺手增量更新镜像(我们本来就知道写了什么);</li>
 *   <li><b>点「同步门禁」</b>: 才真正去设备全量读一次并覆盖镜像。</li>
 * </ul>
 *
 * <h3>隐私约束(刻意为之, 别改)</h3>
 * <ul>
 *   <li>镜像里<b>只有元数据</b>(设备 user_id / 账号 / 姓名 / 是否有人脸 / 来源), <b>不存人脸图</b>;</li>
 *   <li>人脸缩略图单独走 {@code /faces}, 只放进程内存(带 TTL), 既不落库也不落盘;</li>
 *   <li>每次查看都写审计记录, 但 {@code isSaveResponseData=false} —— 响应体含人脸 base64,
 *       而 LogAspect 会把响应前 2000 字符写进 sys_oper_log, 不关掉就是把生物特征写进数据库;</li>
 *   <li>删除必须显式给 userId, 不提供批量接口。</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@Validated
@Api(value = "门禁设备人员管理", tags = {"门禁设备人员管理"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/admin/device")
public class StudyroomDeviceAdminController extends BaseController {

    private final ObjectProvider<IDeviceChannel> deviceChannelProvider;
    private final AccessDeviceMapper accessDeviceMapper;
    /** 删人脸时可选同步清空"我们库里的门禁档案", 让小程序显示未录、可以重新录 */
    private final AccessProfileMapper accessProfileMapper;

    /** 镜像的 Redis key(存元数据 JSON 数组, 不含人脸图) */
    private static final String MIRROR_KEY = "studyroom:access:device:users:";
    /** 人脸缩略图的进程内缓存有效期: 只影响"要不要为看图再问一次设备", 与数据新鲜度无关 */
    private static final long FACE_CACHE_MS = 30 * 60_000L;

    private volatile Map<String, String> faceImgCache = new LinkedHashMap<>();
    private volatile Map<String, String> faceIdCache = new LinkedHashMap<>();
    private volatile long faceCacheAt;
    private volatile String faceCacheSn;
    /** 人脸缓存预热中标记, 避免多个请求同时去读设备 */
    private final java.util.concurrent.atomic.AtomicBoolean warming = new java.util.concurrent.atomic.AtomicBoolean(false);
    /** 全量同步进行中标记, 避免重复触发 */
    private final java.util.concurrent.atomic.AtomicBoolean syncing = new java.util.concurrent.atomic.AtomicBoolean(false);

    /**
     * 设备人员清单。
     *
     * <p><b>默认只读本地镜像, 不碰设备。</b>只有镜像不存在/为空时才会自动去设备同步一次
     * (例如首次使用或 Redis 被清过)。要强制刷新请用 {@code POST /sync}。</p>
     */
    @ApiOperation("设备人员清单(读本地镜像)")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "查看门禁设备人员", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @GetMapping("/users")
    public R<Map<String, Object>> users(@RequestParam(required = false) String sn) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不支持设备人员管理(设备可能未接入)");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        List<Map<String, Object>> list = readMirror(devSn);
        if (list == null) {
            list = new ArrayList<>();
        }
        // 绝对不在这里同步设备! 曾经写成"镜像为空就自动同步", 结果是恶性循环:
        // 同步偶尔读回 0 人 -> 镜像被刷空 -> 每次看页面都触发全量同步(实测 95 秒)
        // -> 前端 axios 10 秒超时 -> nginx 里全是 499 -> 永远打不开。
        // 现在空就是空, 由管理员显式点「同步门禁」。
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sn", devSn);
        out.put("total", list.size());
        long managed = 0L;
        for (Map<String, Object> u : list) {
            if (Boolean.TRUE.equals(u.get("managed"))) {
                managed++;
            }
        }
        out.put("managed", managed);
        out.put("users", list);
        out.put("needSync", list.isEmpty());
        out.put("mirror", true);
        return R.ok(out);
    }

    /**
     * 从门禁设备全量同步一次, 覆盖本地镜像。
     *
     * <p>这是<b>唯一</b>会真正读设备的入口(读页面不会)。厂商渠道录进来的人只能靠它发现。</p>
     */
    @ApiOperation("同步门禁设备人员(真读设备)")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "同步门禁设备人员", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @PostMapping("/sync")
    public R<Map<String, Object>> sync(@RequestParam(required = false) String sn) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不支持设备人员管理(设备可能未接入)");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        // 同步要读 127 人 + 121 张人脸(单页最大 679KB), 实测 95 秒 —— 前端 axios 10 秒就超时。
        // 所以改成后台执行、立刻返回: 管理员看到"已开始同步", 稍后刷新即可看到结果。
        if (!syncing.compareAndSet(false, true)) {
            return R.ok(single("syncing", true));
        }
        final String fSn = devSn;
        new Thread(() -> {
            try {
                syncFromDevice(ch, fSn);
            } catch (Exception e) {
                log.warn("[门禁后台] 同步失败: {}", e.getMessage());
            } finally {
                syncing.set(false);
            }
        }, "access-device-sync").start();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sn", devSn);
        out.put("syncing", true);
        out.put("msg", "已在后台开始同步，约 1~2 分钟后点「刷新列表」即可看到结果");
        return R.ok(out);
    }

    private static Map<String, Object> single(String k, Object v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k, v);
        m.put("msg", "同步正在进行中，请稍后点「刷新列表」");
        return m;
    }

    /**
     * 批量取人脸缩略图(供列表显示)。
     *
     * <p>人脸图不进镜像、不进数据库; 这里只在进程内存里留 30 分钟,
     * 所以正常浏览不会反复去问设备, 过期的代价也只是多读一次。</p>
     */
    @ApiOperation("人脸缩略图(进程内缓存, 不读设备)")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "查看门禁人脸", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @GetMapping("/faces")
    public R<Map<String, Object>> faces(@RequestParam(required = false) String sn) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不可用");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        // 关键: 这里绝不读设备。读全部人脸要走 5 页、单页最大 679KB,
        // 冷启动时能跑几十秒 —— 而前端 axios 默认 10 秒就超时, 页面直接报"系统接口请求超时"。
        // 所以只返回现有缓存; 没缓存就返回空 + warming 标记, 让管理员点「同步门禁」来灌(或等后台预热)。
        boolean warm = devSn.equals(faceCacheSn)
            && (System.currentTimeMillis() - faceCacheAt) < FACE_CACHE_MS
            && !faceImgCache.isEmpty();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("warm", warm);
        out.put("count", faceImgCache.size());
        // 后台「运营操作 → 门禁设备人员」的人脸列是这么取的(studyroom/admin/index.vue loadDeviceFaces):
        //     const map = res.data || {};  if (map[row.userId]) row.faceImg = map[row.userId]
        // 即把 res.data 直接当 uid→base64 映射用, 只认顶层键。
        // 之前把映射塞在 "faces" 子对象里, 页面永远取不到 => 「人脸」列全显示"未录"。
        // 所以这里把映射平铺到顶层(uid 都是数字串, 不会和 warm/count 撞键)。
        out.putAll(faceImgCache);
        if (!warm && !warming.compareAndSet(false, true)) {
            // 已有预热在进行, 直接返回空
            return R.ok(out);
        }
        if (!warm) {
            // 后台预热: 不阻塞本次请求, 下次打开就有图了
            new Thread(() -> {
                try {
                    ensureFaces(ch, devSn, true);
                    log.info("[门禁后台] 人脸缓存预热完成: sn={} 共 {} 张", devSn, faceImgCache.size());
                } catch (Exception e) {
                    log.warn("[门禁后台] 人脸缓存预热失败: {}", e.getMessage());
                } finally {
                    warming.set(false);
                }
            }, "access-face-warmup").start();
        }
        return R.ok(out);
    }

    /** 读取单个人脸原图(base64), 用于放大核对 */
    @ApiOperation("读取单个人脸")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "查看门禁人脸原图", businessType = BusinessType.OTHER, isSaveResponseData = false)
    @GetMapping("/face")
    public R<String> face(@RequestParam String userId, @RequestParam(required = false) String sn) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不可用");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        ensureFaces(ch, devSn, false);
        String img = faceImgCache.get(userId);
        // 缓存冷时只读不到图, 不要为了一张图去跑全量(那会把请求拖到超时)
        return img != null ? R.ok(img) : R.fail("人脸缓存未就绪或该人员未录入人脸，请先点「同步门禁」");
    }

    /**
     * 删除设备上的某个人员。
     *
     * <p>必须显式给出 user_id —— 不提供批量接口, 避免误操作把一屋子人挡在门外。</p>
     */
    @ApiOperation("删除设备人员")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "删除门禁设备人员", businessType = BusinessType.DELETE)
    @DeleteMapping("/user")
    public R<Void> deleteUser(@RequestParam String userId, @RequestParam(required = false) String sn) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不可用");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        boolean ok = ch.deleteDeviceUser(devSn, userId);
        if (ok) {
            // 增量更新镜像: 不必为了一次删除去全量重读设备
            List<Map<String, Object>> list = readMirror(devSn);
            if (list != null) {
                List<Map<String, Object>> kept = new ArrayList<>(list.size());
                for (Map<String, Object> u : list) {
                    if (!userId.equals(String.valueOf(u.get("userId")))) {
                        kept.add(u);
                    }
                }
                writeMirror(devSn, kept);
            }
            faceImgCache.remove(userId);
            faceIdCache.remove(userId);
        }
        return ok ? R.ok() : R.fail("设备未确认删除(可能已不存在)");
    }

    /**
     * 只删除某个人员的人脸, <b>人员保留</b>(账号、权限、其它信息都不动)。
     *
     * <p>与「删除人员」的区别: 那个会把整条人员从设备上删掉(刷不开门且账号消失), 这个只是把他的脸删了 ——
     * 适合"脸录错了要重录"或"不再需要刷脸但账号还要留着"。</p>
     *
     * <p>可选 {@code clearProfile=true}(默认): 同时清空我们库里对应的门禁档案(jobnumber/人脸照片),
     * 这样小程序会显示"未录人脸"、用户可以重新录 —— 否则客户以为录好了却刷不开门, 到店才发现。</p>
     */
    @ApiOperation("删除设备上的某张人脸(人员保留)")
    @SaCheckPermission(value = {"studyroom:accessProfile:list", "studyroom:admin:occupy", "studyroom:cardUser:list"},
        mode = SaMode.OR)
    @Log(title = "删除门禁人脸", businessType = BusinessType.DELETE, isSaveResponseData = false)
    @DeleteMapping("/face")
    public R<String> deleteFace(@RequestParam String userId,
                               @RequestParam(required = false) String sn,
                               @RequestParam(required = false) Boolean clearProfile) {
        IDeviceChannel ch = pickNativeChannel();
        if (ch == null) {
            return R.fail("门禁通道不可用");
        }
        String devSn = StringUtils.isNotBlank(sn) ? sn : firstDeviceSn();
        if (StringUtils.isBlank(devSn)) {
            return R.fail("未配置门禁设备");
        }
        boolean ok = ch.deleteDeviceFace(devSn, userId);
        if (!ok) {
            return R.fail("设备未确认删除人脸(该人员可能本来就没有人脸)");
        }
        // 进程内缓存与镜像同步: 删完列表立刻显示"未录", 不必等下次同步
        faceImgCache.remove(userId);
        faceIdCache.remove(userId);
        updateMirrorFaceFlag(devSn, userId, false);

        String extra = "";
        if (clearProfile == null || clearProfile) {
            extra = clearLocalProfile(devSn, userId);
        }
        return R.ok("已删除该人员的人脸(人员保留)" + (StringUtils.isBlank(extra) ? "" : ", " + extra));
    }

    /** 把镜像里该人员的"有人脸"标记改成 false(增量更新, 不重读设备) */
    private void updateMirrorFaceFlag(String sn, String deviceUserId, boolean hasFace) {
        try {
            List<Map<String, Object>> list = readMirror(sn);
            if (list == null) {
                return;
            }
            boolean changed = false;
            for (Map<String, Object> u : list) {
                if (deviceUserId.equals(String.valueOf(u.get("userId")))) {
                    u.put("hasFace", hasFace);
                    if (!hasFace) {
                        u.put("faceCount", 0);
                    }
                    changed = true;
                }
            }
            if (changed) {
                writeMirror(sn, list);
            }
        } catch (Exception e) {
            log.warn("[门禁后台] 更新镜像人脸标记失败 sn={} uid={}: {}", sn, deviceUserId, e.getMessage());
        }
    }

    /**
     * 清空该设备人员在我们库里的门禁档案(jobnumber / 人脸照片 / 同步状态),
     * 让小程序显示"未录人脸"、用户可重新录。
     * <p>设备账号形如 {@code __sr<我们库的 userId>}, 据此反查; 厂商渠道录的人员我们库里没档案, 直接跳过。</p>
     */
    private String clearLocalProfile(String sn, String deviceUserId) {
        try {
            List<Map<String, Object>> list = readMirror(sn);
            if (list == null) {
                return "";
            }
            String acct = null;
            for (Map<String, Object> u : list) {
                if (deviceUserId.equals(String.valueOf(u.get("userId")))) {
                    acct = str(u.get("userAcct"));
                    break;
                }
            }
            if (acct == null || !acct.startsWith("__sr")) {
                return "";
            }
            long ourUserId;
            try {
                ourUserId = Long.parseLong(acct.substring(4));
            } catch (Exception ignore) {
                return "";
            }
            List<AccessProfile> profiles = accessProfileMapper.selectList(
                Wrappers.<AccessProfile>lambdaQuery().eq(AccessProfile::getUserId, ourUserId));
            if (profiles == null || profiles.isEmpty()) {
                return "";
            }
            int n = 0;
            for (AccessProfile p : profiles) {
                LambdaUpdateWrapper<AccessProfile> uw = Wrappers.lambdaUpdate();
                uw.eq(AccessProfile::getId, p.getId())
                    .set(AccessProfile::getJobnumber, null)
                    .set(AccessProfile::getFacePhotoUrl, null)
                    .set(AccessProfile::getSyncStatus, 0);
                accessProfileMapper.update(null, uw);
                n++;
            }
            log.info("[门禁后台] 已清空门禁档案: 用户={} 档案数={}(删脸后小程序将显示未录)", ourUserId, n);
            return "并清空该会员的门禁档案 " + n + " 条(小程序显示未录, 可重新录)";
        } catch (Exception e) {
            log.warn("[门禁后台] 清空本地门禁档案失败 uid={}: {}", deviceUserId, e.getMessage());
            return "";
        }
    }

    // ------------------------- 镜像读写 -------------------------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readMirror(String sn) {
        try {
            List<Map<String, Object>> v = RedisUtils.getCacheList(MIRROR_KEY + sn);
            return (v == null || v.isEmpty()) ? null : v;
        } catch (Exception e) {
            log.warn("[门禁后台] 读镜像失败 sn={}: {}", sn, e.getMessage());
            return null;
        }
    }

    private void writeMirror(String sn, List<Map<String, Object>> list) {
        try {
            // 必须"先删再写"。RedisUtils.setCacheList 是往列表里追加(RPUSH), 不是覆盖 ——
            // 每次同步都追加 127 个元素, 同步几百次后这个键涨到 9.8 万个元素,
            // 读一次要 9 秒, 前端 10 秒超时、页面直接打不开。实测踩过这个大坑。
            RedisUtils.deleteObject(MIRROR_KEY + sn);
            RedisUtils.setCacheList(MIRROR_KEY + sn, list);
        } catch (Exception e) {
            log.warn("[门禁后台] 写镜像失败 sn={}: {}", sn, e.getMessage());
        }
    }

    /**
     * 增量更新镜像中某个人员的字段(录入人员后调用)。
     *
     * <p>这就是"有录入后更新一下就行"的落点: 我们本来就知道写了什么(user_id/账号/姓名),
     * 完全没必要为了一次录入再去全量重读设备。</p>
     */
    public static void upsertMirrorUser(String sn, String deviceUserId, String userAcct, String userName) {
        if (StringUtils.isBlank(sn) || StringUtils.isBlank(deviceUserId)) {
            return;
        }
        try {
            List<Map<String, Object>> list = RedisUtils.getCacheList(MIRROR_KEY + sn);
            if (list == null) {
                // 镜像还没建立: 交给下次看页面时自动全量同步, 这里不猜
                return;
            }
            boolean found = false;
            for (Map<String, Object> u : list) {
                if (deviceUserId.equals(String.valueOf(u.get("userId")))) {
                    u.put("userName", userName);
                    u.put("userAcct", userAcct);
                    found = true;
                    break;
                }
            }
            if (!found) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("userId", deviceUserId);
                row.put("userName", userName);
                row.put("userAcct", userAcct);
                row.put("faceCount", "1");
                row.put("managed", userAcct != null && userAcct.startsWith("__sr"));
                row.put("source", (userAcct != null && userAcct.startsWith("__sr")) ? "本系统" : "厂商渠道");
                list.add(row);
            }
            RedisUtils.deleteObject(MIRROR_KEY + sn);
            RedisUtils.setCacheList(MIRROR_KEY + sn, list);
        } catch (Exception e) {
            log.warn("[门禁后台] 更新镜像失败 sn={} uid={}: {}", sn, deviceUserId, e.getMessage());
        }
    }

    /**
     * 从设备全量读一次, 覆盖镜像, 并把人脸图灌进内存缓存。
     * 这是唯一真正访问设备的路径(看页面不会到这里)。
     */
    private List<Map<String, Object>> syncFromDevice(IDeviceChannel ch, String devSn) {
        List<Map<String, Object>> users = ch.listDeviceUsers(devSn);
        // 读回空名单绝不覆盖已有镜像: 设备忙/离线时 listDeviceUsers 会返回空列表,
        // 一旦拿它覆盖, 镜像就被刷成"0 人", 页面从此再也打不开(实测踩过这个坑)。
        // 空结果只当作"本次同步没成功", 保留上一次的镜像。
        if (users.isEmpty()) {
            List<Map<String, Object>> old = readMirror(devSn);
            log.warn("[门禁后台] 同步读回 0 人(设备可能忙或离线), 保留原镜像 {} 人, 不覆盖",
                old == null ? 0 : old.size());
            return old == null ? new ArrayList<>() : old;
        }
        ensureFaces(ch, devSn, true);
        List<Map<String, Object>> out = new ArrayList<>(users.size());
        long managedCount = 0L;
        for (Map<String, Object> u : users) {
            String uid = str(u.get("user_id"));
            String acct = str(u.get("user_acct"));
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", uid);
            row.put("userName", str(u.get("user_name")));
            row.put("userAcct", acct);
            row.put("faceCount", str(u.get("face_cnt")));
            row.put("userType", str(u.get("user_type")));
            row.put("authMark", str(u.get("auth_mark")));
            row.put("expOpt", str(u.get("exp_opt")));
            // 只有 __sr 前缀的是本系统建的(预约结束能自动撤销); 其余是厂商渠道录进来的, 系统管不到
            boolean managed = acct != null && acct.startsWith("__sr");
            if (managed) {
                managedCount++;
            }
            row.put("managed", managed);
            row.put("source", managed ? "本系统" : "厂商渠道");
            row.put("faceId", faceIdCache.get(uid));
            row.put("hasFace", faceImgCache.containsKey(uid));
            out.add(row);
        }
        writeMirror(devSn, out);
        log.info("[门禁后台] 已从设备同步: sn={} 人员={} 有人脸={} 本系统={}",
            devSn, out.size(), faceImgCache.size(), managedCount);
        return out;
    }

    /** 确保人脸内存缓存可用; force=true 时强制重读设备 */
    private void ensureFaces(IDeviceChannel ch, String devSn, boolean force) {
        if (!force && devSn.equals(faceCacheSn)
            && (System.currentTimeMillis() - faceCacheAt) < FACE_CACHE_MS
            && !faceImgCache.isEmpty()) {
            return;
        }
        Map<String, String> imgs = new LinkedHashMap<>();
        Map<String, String> ids = new LinkedHashMap<>();
        for (int page = 1; page <= 5; page++) {
            List<Map<String, Object>> faces = ch.listDeviceFaces(devSn, page, 100);
            if (faces.isEmpty()) {
                break;
            }
            for (Map<String, Object> f : faces) {
                String uid = str(f.get("user_id"));
                if (uid == null) {
                    continue;
                }
                String img = str(f.get("face_img"));
                if (img != null && imgs.get(uid) == null) {
                    imgs.put(uid, img);
                }
                String fid = str(f.get("face_id"));
                if (fid != null && ids.get(uid) == null) {
                    ids.put(uid, fid);
                }
            }
            if (faces.size() < 100) {
                break;
            }
        }
        faceImgCache = imgs;
        faceIdCache = ids;
        faceCacheAt = System.currentTimeMillis();
        faceCacheSn = devSn;
    }

    private IDeviceChannel pickNativeChannel() {
        for (IDeviceChannel c : deviceChannelProvider.orderedStream()
            .collect(java.util.stream.Collectors.toList())) {
            try {
                if (c.supportsNativeUserSync()) {
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String firstDeviceSn() {
        try {
            List<AccessDevice> list = accessDeviceMapper.selectList(null);
            if (list != null) {
                for (AccessDevice d : list) {
                    if (d != null && StringUtils.isNotBlank(d.getSn())) {
                        return d.getSn();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[门禁后台] 查询门禁设备失败: {}", e.getMessage());
        }
        return null;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
