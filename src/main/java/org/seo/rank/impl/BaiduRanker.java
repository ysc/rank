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

package org.seo.rank.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.Ranker;
import org.seo.rank.list.UrlTools;
import org.seo.rank.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 判断网页是否被搜索引擎收录
 * @author 杨尚川
 */
public class BaiduRanker implements Ranker{
    private static final Logger LOGGER = LoggerFactory.getLogger(BaiduRanker.class);
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String HOST = "www.baidu.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0";
    
    //获取多少页
    private static final int PAGE = 5;
    private static final int PAGESIZE = 10;

    @Override
    public void rank(List<Rank> ranks) {
        for(Rank rank : ranks){
            rank(rank);
        }
    }
    @Override
    public void rank(Rank rank){
        doRank(rank);
    }
    /**
     * 查询网页在百度的排名
     * @param rank 排名数据结构
     */
    public void doRank(Rank rank){
        if(StringUtils.isBlank(rank.getKeyword()) || StringUtils.isBlank(rank.getUrl())){
            return ;
        }
        //检查是否被百度收录
        searchBaiduIndex(rank);
        //检查百度排名
        String query = null;
        try {
            query = URLEncoder.encode(rank.getKeyword(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("url构造失败", e);
            return ;
        }
        if(StringUtils.isBlank(query)){
            return ;
        }
        for (int i = 0; i < PAGE; i++) {
            String path = "http://www.baidu.com/s?tn=monline_5_dg&ie=utf-8&wd=" + query+"&oq="+query+"&usm=3&f=8&bs="+query+"&rsv_bp=1&rsv_sug3=1&rsv_sug4=141&rsv_sug1=1&rsv_sug=1&pn=" + i * PAGESIZE;
            LOGGER.debug(path);
            int r = searchBaiduRank(path, rank.getUrl());
            if (r > 0){
                rank.setRank(r+i*10);
                //找到排名
                return;
            }
        }
    }
    /**
     * 检查百度是否收录
     * @param rank 
     */
    private void searchBaiduIndex(Rank rank) {
        String url = "url:"+rank.getUrl();
        url = "http://www.baidu.com/s?wd=" + url;
        try {
            Document document = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("User-Agent", USER_AGENT)
                    .header("Host", HOST)
                    .get();
            
            String notFoundCssQuery = "html body div div div div div.nors p";
            Elements elements = document.select(notFoundCssQuery);
            for(Element element : elements){
                String text = element.text();
                if(text.contains("抱歉，没有找到与") && text.contains("相关的网页。")){
                    //未被百度收录
                    rank.setIndex(false);
                    return;
                }
            }
            String numberCssQuery = "html body div div div p#page span.nums";
            elements = document.select(numberCssQuery);
            for(Element element : elements){
                String text = element.text();
                if(text.equals("百度为您找到相关结果约1个")){
                    //百度收录
                    rank.setIndex(true);
                    return;
                }
            }
        } catch (IOException ex) {
            LOGGER.error("搜索出错",ex);
        }
    }
    /**
     * 检查百度排名
     * @param url 检查百度的URL
     * @param targetUrl 网页的URL
     * @return 
     */
    private int searchBaiduRank(String url, String targetUrl) {
        try {
            Document document = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("User-Agent", USER_AGENT)
                    .header("Host", HOST)
                    .get();
            String titleCssQuery = "html body div div div div div h3.t a";
            Elements elements = document.select(titleCssQuery);
            int i=0;
            for(Element element : elements){
                if(StringUtils.isBlank(element.text())){
                    continue;
                }
                i++;
                LOGGER.debug(i+":"+element.text());
                String href = element.attr("href");
                href = UrlTools.normalizeUrl(url, href);
                String realUrl = urlConvert(href);
                LOGGER.debug("url:"+url);
                LOGGER.debug("realUrl:"+realUrl);
                LOGGER.debug("targetUrl:"+targetUrl);
                if(targetUrl.equals(realUrl)){
                    return i;
                }
            }
        } catch (Exception ex) {
            LOGGER.error("搜索出错",ex);
        }
        return -1;
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
            String realUrl = Jsoup.connect(url)
                        .header("Accept", ACCEPT)
                        .header("Accept-Encoding", ENCODING)
                        .header("Accept-Language", LANGUAGE)
                        .header("Connection", CONNECTION)
                        .header("User-Agent", USER_AGENT)
                        .header("Host", HOST)
                        //百度会返回302临时重定向，重定向到真实的网页地址
                        .followRedirects(false)
                        .execute()
                        .header("Location");
            //还要检查网页是否被重定向
            
            return realUrl;
        }catch(Exception e){
            LOGGER.error("URL转换异常", e);
        }
        return url;
    }
    public static void main(String[] args){
        BaiduRanker ranker = new BaiduRanker();
        
        Rank rank = new Rank();
        rank.setKeyword("Java应用级产品开发平台APDPlat作者杨尚川专访");
        rank.setUrl("http://www.iteye.com/magazines/113");
        ranker.searchBaiduIndex(rank);
        System.out.println(rank);
        
        rank = new Rank();
        rank.setKeyword("Java应用级产品开发平台APDPlat作者杨尚川专访");
        rank.setUrl("http://www.iteye.com/magazines/113");
        ranker.rank(rank);
        System.out.println(rank);
        
        rank = new Rank();
        rank.setKeyword("QuestionAnsweringSystem v1.1 发布，人机问答系统");
        rank.setUrl("http://yangshangchuan.iteye.com/blog/2101533");
        ranker.searchBaiduIndex(rank);
        System.out.println(rank);
        
        rank = new Rank();
        rank.setKeyword("天天向上");
        rank.setUrl("http://www.manmankan.com/dy2013/zongyi/201306/6.shtml");
        ranker.rank(rank);
        System.out.println(rank);
        
    }
}
