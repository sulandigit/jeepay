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
package com.jeequan.jeepay.core.cache;

import com.jeequan.jeepay.core.constants.CS;
import com.jeequan.jeepay.core.model.security.JeeUserDetails;

/**
 * token服务类
 *
 * @author terrfly
 * @site https://www.jeequan.com
 * @date 2021/5/24 09:06
 */
public class ITokenService {

    /**
     * 处理token信息
     * 1. 如果不允许多用户则踢掉之前的所有用户信息
     * 2. 更新token 缓存时间信息
     * 3. 更新用户token列表
     * 
     * @param userDetail 用户详情对象
     * @param cacheKey 缓存键
     * @date 2021
     */
    public static void processTokenCache(JeeUserDetails userDetail, String cacheKey){

        userDetail.setCacheKey(cacheKey);

        //当前用户的所有登录token 集合
//        if(!PropKit.isAllowMultiUser()){ //不允许多用户登录
//
//            List<String> allTokenList = new ArrayList<>();
//            for (String token : allTokenList) {
//                if(!cacheKey.equalsIgnoreCase(token)){
//                    RedisUtil.del(token);
//                }
//            }
//        }

        RedisUtil.set(cacheKey, userDetail, CS.TOKEN_TIME);
    }


    /**
     * 退出时，清除token信息
     * 
     * @param iToken 用户token
     * @param currentUID 当前用户ID
     * @date 2021
     */
    public static void removeIToken(String iToken, Long currentUID){

        RedisUtil.del(iToken);
    }

    /**
     * 刷新用户缓存数据
     * 
     * @param currentUserInfo 当前用户信息
     * @date 2021
     */
    public static void refData(JeeUserDetails currentUserInfo){

        RedisUtil.set(currentUserInfo.getCacheKey(), currentUserInfo, CS.TOKEN_TIME);

    }

}