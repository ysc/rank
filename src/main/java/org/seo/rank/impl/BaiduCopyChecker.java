/*
 * *
 *  *
 *  * APDPlat - Application Product Development Platform
 *  * Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  *
 *
 */

package org.seo.rank.impl;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.CopyChecker;
import org.seo.rank.list.DynamicIp;
import org.seo.rank.list.UrlTools;
import org.seo.rank.list.impl.DefaultParser;
import org.seo.rank.model.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 检查文章抄袭情况
 * @author 杨尚川
 */
public class BaiduCopyChecker implements CopyChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduCopyChecker.class);
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String HOST = "www.baidu.com";
    private static final String REFERER = "http://www.baidu.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0";
    
    //获取多少页
    private static final int PAGE = 15;
    private static final int PAGESIZE = 10;


    @Override
    public Map<Article, Set<String>> check(List<Article> articles) {
        Map<Article, Set<String>> data = new HashMap<>();
        articles.forEach(article -> {
            data.put(article, doCheck(article));
        });
        return data;
    }

    public Set<String> doCheck(Article article){
        Set<String> data = new HashSet<>();
        if(StringUtils.isBlank(article.getTitle())
                || StringUtils.isBlank(article.getUrl())){
            return data;
        }
        String query = null;
        try {
            query = URLEncoder.encode(article.getTitle(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("url构造失败", e);
            return data;
        }
        if(StringUtils.isBlank(query)){
            return data;
        }
        for (int i = 0; i < PAGE; i++) {
            String url = "http://www.baidu.com/s?tn=monline_5_dg&ie=utf-8&wd=" + query+"&oq="+query+"&usm=3&f=8&bs="+query+"&rsv_bp=1&rsv_sug3=1&rsv_sug4=141&rsv_sug1=1&rsv_sug=1&pn=" + i * PAGESIZE;
            LOGGER.debug(url);
            data.addAll(doCheck(url, article));
        }
        return data;
    }

    private Set<String> doCheck(String url, Article article) {
        Set<String> data = new HashSet<>();
        try {
            Document document = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Host", HOST)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .get();
            String titleCssQuery = "html body div div div div div h3.t a";
            Elements elements = document.select(titleCssQuery);
            int i=0;
            for(Element element : elements){
                String _title = element.text();
                if(StringUtils.isBlank(_title)){
                    continue;
                }
                i++;
                LOGGER.debug(i+":"+_title);
                if(!_title.contains(article.getTitle())){
                    LOGGER.debug("搜索结果检查通过");
                    continue;
                }
                String href = element.attr("href");
                href = UrlTools.normalizeUrl(url, href);
                String realUrl = urlConvert(href);
                LOGGER.debug("url:"+url);
                LOGGER.debug("realUrl:"+realUrl);
                String[] target = new URL(realUrl).getHost().split("\\.");
                String[] source = new URL(article.getUrl()).getHost().split("\\.");
                if(target.length>1
                        && source.length>1
                        && !(target[target.length-2]+target[target.length-1]).equals(source[source.length-2]+source[source.length-1])) {
                    data.add(realUrl);
                }
            }
        } catch (Exception ex) {
            LOGGER.error("搜索出错",ex);
        }
        return data;
    }
    /**
     * 将百度的链接转换为网页的链接
     * @param url 百度链接
     * @return 网页链接
     */
    private static String urlConvert(String url){
        try{
            if(!url.startsWith("http://www.baidu.com/link?url=")){
                //不需要转换URL
                return url;
            }
            LOGGER.debug("转换前的URL："+url);
            Connection.Response response = getResponse(url);
            //这里要处理爬虫限制
            if(response==null || response.body().contains("请您点击按钮解除封锁")
                    || response.body().contains("请输入以下验证码")){
                //使用新的IP地址
                DynamicIp.toNewIp();
                response = getResponse(url);
            }
            String realUrl = response.header("Location");
            LOGGER.debug("转换后的URL："+realUrl);
            //检查网页是否被重定向
            //这个检查会导致速度有点慢
            //这个检测基本没有必要，除非是那种极其特殊的网站，ITEYE曾经就是，后来在我的建议下改进了
            /*
            LOGGER.debug("检查是否有重定向："+realUrl);
            Connection.Response response = getResponse(realUrl);
            //这里要处理爬虫限制
            if(response==null || response.body().contains("请您点击按钮解除封锁")
                              || response.body().contains("请输入以下验证码")){
                //使用新的IP地址
                DynamicIp.toNewIp();
                response = getResponse(realUrl);
            }
            String realUrl2 = response.header("Location");
            if(!StringUtils.isBlank(realUrl2)){
                LOGGER.debug("检查到重定向到："+realUrl2);
                return realUrl2;
            }
            */
            return realUrl;
        }catch(Exception e){
            LOGGER.error("URL转换异常", e);
        }
        return url;
    }
    private static Connection.Response getResponse(String url) {
        try{
            Connection.Response response = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Host", HOST)
                    .header("Referer", REFERER)
                    .header("User-Agent", USER_AGENT)
                    .ignoreContentType(true)
                    .timeout(30000)
                    .followRedirects(false)
                    .execute();
            return response;
        } catch (Exception e){
            LOGGER.debug("获取页面失败：", e);
        }
        return null;
    }
    public static void main(String[] args){
        CopyChecker copyChecker = new BaiduCopyChecker();
        //计算OSCHINA博文被抄袭的情况
        //List<Article> articles = DefaultParser.oschinaBlog();
        //计算ITEYE博文被抄袭的情况
        List<Article> articles = DefaultParser.iteyeBlog();
        //检查
        Map<Article, Set<String>> result = copyChecker.check(articles);
        //输出检查报告
        LOGGER.info("检查博文数目：" + articles.size()+"<br/>");
        AtomicInteger i = new AtomicInteger();
        result.entrySet().stream().sorted((a,b)->b.getValue().size()-a.getValue().size()).forEach(e -> {
            String query = null;
            try {
                query = URLEncoder.encode(e.getKey().getTitle(), "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                LOGGER.error("url构造失败", ex);
                return;
            }
            LOGGER.info(i.incrementAndGet()+"、<a target=\"_blank\" href=\"http://www.baidu.com/s?wd=" + query + "\">" + e.getKey().getTitle() + "</a><br/>");
            if(e.getValue().size()>0) {
                LOGGER.info("抄袭链接有("+e.getValue().size()+")个，分别是：");
                LOGGER.info("<ol>");
                e.getValue().stream().sorted().forEach(url-> LOGGER.info("<li><a target=\"_blank\" href=\"" + url + "\">" + url + "</a></li>"));
                LOGGER.info("</ol>");
            }else{
                LOGGER.info("无抄袭链接");
            }
        });
    }
}
