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
package com.jeequan.jeepay.components.oss.service;

import com.jeequan.jeepay.components.oss.constant.OssSavePlaceEnum;
import org.springframework.web.multipart.MultipartFile;

/**
 * OSS Service Interface
 * OSS服务接口
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/12 18:18
 */
public interface IOssService {

    /** 
     * Upload file and generate download/preview URL
     * 上传文件 & 生成下载/预览URL 
     **/
    String upload2PreviewUrl(OssSavePlaceEnum ossSavePlaceEnum, MultipartFile multipartFile, String saveDirAndFileName);

    /** 
     * Download file to local
     * 将文件下载到本地
     * 
     * @return true if download successful, false if failed or file does not exist
     * @return 是否写入成功 (false: 写入失败， 或者文件不存在)
     * **/
    boolean downloadFile(OssSavePlaceEnum ossSavePlaceEnum, String source, String target);

}
