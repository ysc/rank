/**
 * 
 * APDPlat - Application Product Development Platform
 * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package org.seo.rank.list;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * @author 杨尚川
 */
public class UrlTools {
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0";
    
    private UrlTools(){}
    /**
     * 将本页的非完整URL转换为完整的URL
     * @param url 本页的URL
     * @param href 本页上的相对或绝对非完整URL
     * @return 完整的URL
     * @throws MalformedURLException 
     */
    public static String normalizeUrl(String url, String href) throws MalformedURLException {
        URL u = new URL(url);
        String port = "";
        if(u.getPort() > 0){
            port = ":"+port;
        }
        String host = u.getProtocol()+"://"+u.getHost()+port;
        if (!href.startsWith("http")) {
            //处理非完整路径
            if (href.startsWith("//")) {
                //处理绝对路径
                href = "http:" + href;
            }else if (href.startsWith("/")) {
                //处理绝对路径
                href = host + href;
            }else if(href.startsWith("?")) {
                //处理页面参数
                int index = url.indexOf("?");
                if(index > 0){
                    String temp = url.substring(0, index);
                    href = temp + href;
                }else{
                    href = url + href;
                }
            } else {
                //处理相对路径
                String temp = url;
                int index = url.lastIndexOf("/");
                if (index > 7) {
                    //非协议后面的//
                    //如：http://yangshangchuan.iteye.com/
                    temp = url.substring(0, index + 1);
                } else {
                    temp += "/";
                }
                href = temp + href;
            }
        }
        return href;
    }
}
