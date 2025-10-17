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
package com.jeequan.jeepay.components.oss.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Aliyun OSS YML Configuration
 * 阿里云OSS的YAML配置参数
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/7/12 18:18
 */
@Data
@Component
@ConfigurationProperties(prefix="isys.oss.aliyun-oss")
public class AliyunOssYmlConfig {

	/** Endpoint / 访问端点 */
	private String endpoint;
	
	/** Public bucket name / 公共存储桶名称 */
	private String publicBucketName;
	
	/** Private bucket name / 私有存储桶名称 */
	private String privateBucketName;
	
	/** Access key ID / 访问Key ID */
	private String accessKeyId;
	
	/** Access key secret / 访问Key密钥 */
	private String accessKeySecret;
}



