package com.qmz.canteen.service;

import com.qmz.canteen.dto.StallRequest;
import com.qmz.canteen.vo.StallVO;
import com.qmz.canteen.vo.StallWindowVO;

import java.util.List;

public interface StallService {

    List<StallVO> listWithStats(String keyword, String area, Integer floor);

    /**
     * 档口列表 + 统计。
     *
     * @param dishLimit 不为空时，每个档口额外返回该档口评分最高的若干菜品（topDishes），
     *                  供美食探索「档口排序」页一行档口 + 右侧热评菜品展示
     */
    List<StallVO> listWithStats(String keyword, String area, Integer floor, Integer dishLimit);

    StallVO getStallDetail(Long id);

    /** 某区域楼层的已占用窗口编号（含歇业档口），用于编号选择器 */
    List<StallWindowVO> listWindowNos(String area, Integer floor);

    Long createStall(StallRequest request);

    void updateStall(Long id, StallRequest request);

    void deleteStall(Long id);

    /** 管理员重置某档口商家账号密码 */
    void resetMerchantPassword(Long stallId, String password);

    /** 商家更新自己档口的健康证/营业执照 */
    void updateCertificates(Long stallId, String healthCertUrl, String licenseUrl);
}
