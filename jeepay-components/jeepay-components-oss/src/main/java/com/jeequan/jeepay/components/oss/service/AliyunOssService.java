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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GetObjectRequest;
import com.jeequan.jeepay.components.oss.config.AliyunOssYmlConfig;
import com.jeequan.jeepay.components.oss.constant.OssSavePlaceEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;

/**
 * Aliyun OSS Service Implementation
 * 阿里云OSS实现类
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/12 18:20
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "isys.oss.service-type", havingValue = "aliyun-oss")
public class AliyunOssService implements IOssService{

    @Autowired private AliyunOssYmlConfig aliyunOssYmlConfig;

    /** OSS Client initialization / OSS客户端初始化 */
    private OSS ossClient = null;

    @PostConstruct
    public void init(){
        ossClient = new OSSClientBuilder().build(aliyunOssYmlConfig.getEndpoint(), aliyunOssYmlConfig.getAccessKeyId(), aliyunOssYmlConfig.getAccessKeySecret());
    }

    @Override
    public String upload2PreviewUrl(OssSavePlaceEnum ossSavePlaceEnum, MultipartFile multipartFile, String saveDirAndFileName) {

        try {

            // Upload to public or private bucket / 上传到公有或私有bucket
            this.ossClient.putObject(ossSavePlaceEnum == OssSavePlaceEnum.PUBLIC ? aliyunOssYmlConfig.getPublicBucketName() : aliyunOssYmlConfig.getPrivateBucketName()
                    , saveDirAndFileName, multipartFile.getInputStream());

            if(ossSavePlaceEnum == OssSavePlaceEnum.PUBLIC){
                // Documentation: https://www.alibabacloud.com/help/zh/doc-detail/39607.htm  
                // example: https://BucketName.Endpoint/ObjectName
                return "https://" + aliyunOssYmlConfig.getPublicBucketName() + "." + aliyunOssYmlConfig.getEndpoint() + "/" + saveDirAndFileName;
            }

            return saveDirAndFileName;

        } catch (Exception e) {
            log.error("error", e);
            return null;
        }
    }

    @Override
    public boolean downloadFile(OssSavePlaceEnum ossSavePlaceEnum, String source, String target) {

        try {

            // Select bucket based on storage location / 根据存储位置选择bucket
            String bucket = ossSavePlaceEnum == OssSavePlaceEnum.PRIVATE ? aliyunOssYmlConfig.getPrivateBucketName() : aliyunOssYmlConfig.getPublicBucketName();
            this.ossClient.getObject(new GetObjectRequest(bucket, source), new File(target));

            return true;
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }
    }

}
