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

package org.seo.rank.list.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.list.Parser;
import org.seo.rank.list.UrlTools;
import org.seo.rank.model.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author 杨尚川
 */
public class DefaultParser implements Parser{
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultParser.class);
    private static final String ACCEPT = "text/html, */*; q=0.01";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.9; rv:31.0) Gecko/20100101 Firefox/31.0";
    
    @Override
    public List<Article> parse(String url, String nextPageCssQuery, String nextPageText, String titleCssQuery) {
        List<Article> articles = new ArrayList<>();
        try{
            Document document = Jsoup.connect(url)
                        .header("Accept", ACCEPT)
                        .header("Accept-Encoding", ENCODING)
                        .header("Accept-Language", LANGUAGE)
                        .header("Connection", CONNECTION)
                        .header("User-Agent", USER_AGENT)
                        .get();
            Elements elements = document.select(titleCssQuery);
            for(Element element : elements){
                String title = element.text();
                String href = element.attr("href");
                if(!StringUtils.isBlank(title) && !StringUtils.isBlank(href)){
                    href = UrlTools.normalizeUrl(url, href);
                    Article article = new Article();
                    article.setTitle(title);
                    article.setUrl(href);
                    articles.add(article);
                }else{
                    LOGGER.info("解析列表页出错："+url+" title:"+title+", href:"+href);
                }
            }
            //获取下一页链接地址
            String nextPageUrl = getNextPageUrl(document, nextPageCssQuery, nextPageText);
            LOGGER.debug("下一页链接："+nextPageUrl);
            if(nextPageUrl != null){
                nextPageUrl = UrlTools.normalizeUrl(url, nextPageUrl);
                LOGGER.debug("规范化后的下一页链接："+nextPageUrl);
                //解析下一页
                List<Article> result = parse(nextPageUrl, nextPageCssQuery, nextPageText, titleCssQuery);
                articles.addAll(result);
            }else{
                LOGGER.info("列表页解析完毕，最后一页："+url);
            }
        }catch(Exception e){
            LOGGER.error("解析列表页出错："+url, e);
        }
        return articles;
    }
    /**
     * 获取下一页链接地址
     * @param document 本页文档对象
     * @param nextPageCssQuery 获取下一页的CSS路径
     * @param nextPageText 下一页CSS路径对应的元素的文本值
     * @return 下一页链接地址
     */
    private String getNextPageUrl(Document document, String nextPageCssQuery, String nextPageText){
        Elements elements = document.select(nextPageCssQuery);
        for(Element element : elements){
            String text = element.text();
            LOGGER.debug(text);
            if(text != null && nextPageText.trim().equals(text.trim())){
                String href = element.attr("href");
                return href;
            }
        }
        return null;
    }
    public static List<Article> run(String url, String nextPageCssQuery, String nextPageText, String titleCssQuery){
        Parser parser = new DefaultParser();
        long start = System.currentTimeMillis();
        List<Article> articles = parser.parse(url, nextPageCssQuery, nextPageText, titleCssQuery);
        long cost = System.currentTimeMillis() - start;
        int i=1;
        for(Article article : articles){
            LOGGER.info((i++) + "、" + article.getTitle() + " : " + article.getUrl());
        }
        LOGGER.info("采集文章 " + articles.size() + " 篇耗时：" + cost / 1000.0 + " 秒");
        return articles;
    }
    public static List<Article> iteyeBlog(){
        String url = "http://yangshangchuan.iteye.com/";
        String nextPageCssQuery = "html body div#page div#content.clearfix div#main div.pagination a.next_page";
        String nextPageText = "下一页 »";
        String titleCssQuery = "html body div#page div#content.clearfix div#main div.blog_main div.blog_title h3 a";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> iteyeNews(){
        String url = "http://www.iteye.com/news";
        String nextPageCssQuery = "html body div#page div#content.clearfix div#main div#index_main div.pagination a.next_page";
        String nextPageText = "下一页 »";
        //h3 > a表示h3后直接跟着a，这样 h3 span.category a 就不会被选择
        String titleCssQuery = "html body div#page div#content.clearfix div#main div#index_main div.news.clearfix div.content h3 > a";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> iteyeMagazines(){
        String url = "http://www.iteye.com/magazines";
        String nextPageCssQuery = "html body div#page div#content.clearfix div#main div#index_main div.pagination a.next_page";
        String nextPageText = "下一页 »";
        String titleCssQuery = "html body div#page div#content.clearfix div#main div#index_main div.news.clearfix div.content h3 a";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> csdnBlog(){
        String url = "http://blog.csdn.net/iispring";
        String nextPageCssQuery = "html body div#container div#body div#main div.main div#papelist.pagelist a";
        String titleCssQuery = "html body div#container div#body div#main div.main div#article_list.list div.list_item.article_item div.article_title h1 span.link_title a";
        String nextPageText = "下一页";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> oschinaNews(){
        String url = "http://www.oschina.net/news";
        String nextPageCssQuery = "html body div#OSC_Screen div#OSC_Content.CenterDiv div#NewsChannel.Channel div#NewsList.ListPanel div#RecentNewsList.panel ul.pager li.page.next a";
        String titleCssQuery = "html body div#OSC_Screen div#OSC_Content.CenterDiv div#NewsChannel.Channel div#NewsList.ListPanel div#RecentNewsList.panel ul.List li h2 a";
        String nextPageText = ">";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> oschinaBlog(){
        String url = "http://my.oschina.net/apdplat/blog";
        String nextPageCssQuery = "html body div#OSC_Screen div#OSC_Content div.SpaceList.BlogList ul.pager li.page.next a";
        String titleCssQuery = "html body div#OSC_Screen div#OSC_Content div.SpaceList.BlogList ul li.Blog div.BlogTitle div.title h2 a";
        String nextPageText = ">";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }
    public static List<Article> baidu(String query){
        //对查询词进行编码
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOGGER.error("url构造失败", e);
            return Collections.emptyList();
        }
        if(StringUtils.isBlank(query)){
            return Collections.emptyList();
        }
        String url = "http://www.baidu.com/s?wd=" + query;
        String nextPageCssQuery = "html body div div div p#page a.n";
        String titleCssQuery = "html body div div div div div h3.t a";
        String nextPageText = "下一页>";
        return run(url, nextPageCssQuery, nextPageText, titleCssQuery);
    }

    /**
     * 比较我的OSCHINA博客和ITEYE博客的异同
     */
    public static void blogCompare(){
        List<Article> ob = oschinaBlog();
        List<Article> ib = iteyeBlog();
        Map<String, String> om = new HashMap<>();
        Map<String, String> im = new HashMap<>();
        ob.stream().forEach(b->om.put(b.getTitle(), b.getUrl()));
        ib.stream().forEach(b->im.put(b.getTitle(), b.getUrl()));
        List<String> iteyeBlog   = ib.stream().map(b -> b.getTitle().replace("[置顶]", "").trim()).sorted().collect(Collectors.toList());
        List<String> oschinaBlog = ob.stream().map(b -> b.getTitle()).sorted().collect(Collectors.toList());

        List<String> commons = oschinaBlog.stream().filter(b -> iteyeBlog.contains(b)).collect(Collectors.toList());
        LOGGER.info("<h4>oschina和iteye都有("+commons.size()+")：</h4>");
        AtomicInteger j = new AtomicInteger();
        commons.forEach(item -> LOGGER.info(j.incrementAndGet()+"、"+item+"    <a target=\"_blank\" href=\""+om.get(item)+"\">oschina</a>    <a target=\"_blank\" href=\""+im.get(item)+"\">iteye</a><br/>"));

        List<String> oschina = oschinaBlog.stream().filter(i -> !iteyeBlog.contains(i)).collect(Collectors.toList());
        LOGGER.info("<h4>oschina独有("+oschina.size()+")：</h4>");
        AtomicInteger l = new AtomicInteger();
        oschina.forEach(item -> LOGGER.info(l.incrementAndGet()+"、<a target=\"_blank\" href=\""+om.get(item)+"\">"+item+"</a><br/>"));

        List<String> iteye = iteyeBlog.stream().filter(i -> !oschinaBlog.contains(i)).collect(Collectors.toList());
        LOGGER.info("<h4>iteye独有("+iteye.size()+")：</h4>");
        AtomicInteger k = new AtomicInteger();
        iteye.forEach(item -> LOGGER.info(k.incrementAndGet()+"、<a target=\"_blank\" href=\""+im.get(item)+"\">"+item+"</a><br/>"));
    }
    public static void main(String[] args){
        //iteyeBlog();
        //iteyeNews();
        //iteyeMagazines();
        //csdnBlog();
        //oschinaNews();
        //oschinaBlog();
        //baidu("Java应用级产品开发平台APDPlat作者杨尚川专访");
        blogCompare();
    }
}
