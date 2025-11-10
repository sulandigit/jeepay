/*
 * Copyright (c) 2021-2031, 河北计全科技有限公司 (https://www.jeequan.com & jeequan@126.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeequan.jeepay.core.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * <p>
 * 游标分页结果包装类
 * 用于替代传统的offset分页，解决深分页性能问题
 * </p>
 *
 * @author optimization
 * @since 2025-10-15
 */
@ApiModel(value = "游标分页结果", description = "游标分页查询结果封装")
@Data
public class CursorPageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 数据列表
     */
    @ApiModelProperty(value = "数据列表")
    private List<T> records;

    /**
     * 下一页游标（最后一条记录的ID）
     * 客户端下次请求时需要传递此值
     */
    @ApiModelProperty(value = "下一页游标")
    private String nextCursor;

    /**
     * 是否还有下一页
     */
    @ApiModelProperty(value = "是否还有下一页")
    private boolean hasNext;

    /**
     * 当前返回的记录数
     */
    @ApiModelProperty(value = "当前返回的记录数")
    private int size;

    /**
     * 每页请求的大小
     */
    @ApiModelProperty(value = "每页大小")
    private int pageSize;

    /**
     * 构造函数
     * @param records 查询结果列表
     * @param pageSize 每页大小
     */
    public CursorPageResult(List<T> records, int pageSize) {
        this.records = records;
        this.pageSize = pageSize;
        this.size = records != null ? records.size() : 0;
        
        // 如果返回的记录数等于请求的页大小，说明可能还有下一页
        // 此时需要设置nextCursor为最后一条记录的ID
        if (this.size >= pageSize && this.size > 0) {
            this.hasNext = true;
            // nextCursor需要在业务层设置，因为这里无法获取ID字段
        } else {
            this.hasNext = false;
            this.nextCursor = null;
        }
    }

    /**
     * 设置下一页游标
     * @param nextCursor 游标值
     */
    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    /**
     * 静态工厂方法
     * @param records 记录列表
     * @param pageSize 页大小
     * @param <T> 泛型类型
     * @return 游标分页结果
     */
    public static <T> CursorPageResult<T> of(List<T> records, int pageSize) {
        return new CursorPageResult<>(records, pageSize);
    }
}
