package com.alvis.media.controller.admin;

import com.alvis.media.base.BaseApiController;
import com.alvis.media.base.RestResponse;
import com.alvis.media.base.SystemCode;
import com.alvis.media.domain.User;
import com.alvis.media.service.DatasetService;
import com.alvis.media.viewmodel.dataset.DatasetResponseVM;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 数据集管理接口。
 *
 * RESTful 设计：
 *   POST   /api/admin/dataset/upload   上传数据集（multipart）
 *   GET    /api/admin/dataset/list     列表查询
 *   GET    /api/admin/dataset/{id}     详情
 *   PUT    /api/admin/dataset/{id}     修改名称 / 备注
 *   DELETE /api/admin/dataset/{id}     删除（同时清理磁盘文件）
 */
@RestController("AdminDatasetController")
@RequestMapping("/api/admin/dataset")
@AllArgsConstructor
public class DatasetController extends BaseApiController {

    private final DatasetService datasetService;

    @PostMapping("/upload")
    public RestResponse<DatasetResponseVM> upload(@RequestParam("file") MultipartFile file,
@RequestParam(required = false) String name,
                                                  @RequestParam(required = false) String remark) {
        User current = getCurrentUser();
        if (current == null) {
            return RestResponse.fail(SystemCode.UNAUTHORIZED.getCode(), "用户未登录");
        }
        return RestResponse.ok(datasetService.upload(file, name, remark, current.getId()));
    }

    @GetMapping("/list")
    public RestResponse<List<DatasetResponseVM>> list() {
        return RestResponse.ok(datasetService.list());
    }

    @GetMapping("/{datasetId}")
    public RestResponse<DatasetResponseVM> detail(@PathVariable Integer datasetId) {
        DatasetResponseVM vm = datasetService.detail(datasetId);
        if (vm == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "数据集不存在");
        }
        return RestResponse.ok(vm);
    }

    @PutMapping("/{datasetId}")
    public RestResponse<DatasetResponseVM> update(@PathVariable Integer datasetId,
@RequestParam(required = false) String name,
                                                   @RequestParam(required = false) String remark) {
        DatasetResponseVM vm = datasetService.update(datasetId, name, remark);
        if (vm == null) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "数据集不存在");
        }
        return RestResponse.ok(vm);
    }

    @DeleteMapping("/{datasetId}")
    public RestResponse<Void> delete(@PathVariable Integer datasetId) {
        if (!datasetService.delete(datasetId)) {
            return RestResponse.fail(SystemCode.InnerError.getCode(), "数据集不存在");
        }
        return RestResponse.ok();
    }
}
