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

import org.apdplat.word.WordSegmenter;
import org.apdplat.word.segmentation.Word;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.seo.rank.SimilarChecker;
import org.seo.rank.list.DynamicIp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ITEYE博文相似性检测
 * @author 杨尚川
 */
public class ITEYEBlogSimilarChecker implements SimilarChecker{
    private static final Logger LOGGER = LoggerFactory.getLogger(ITEYEBlogSimilarChecker.class);
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String REFERER = "http://yangshangchuan.iteye.com";
    private static final String HOST = "yangshangchuan.iteye.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:36.0) Gecko/20100101 Firefox/36.0";
    private static final String BLOG_CSS_PATH = "html body div#page div#content.clearfix div#main div.blog_main";
    private static final String BLOG_TITLE_CSS_PATH = "div.blog_title";
    private static final String BLOG_CONTENT_CSS_PATH = "div#blog_content.blog_content";
    private static final float THRESHOLD_RATE = 0.8F;

    @Override
    public boolean isSimilar(String url1, String url2) {
        Blog blog1 = getBlog(url1);
        if(blog1!=null) {
            Blog blog2 = getBlog(url2);
            return check(blog1, blog2);
        }
        return false;
    }

    private boolean check(Blog blog1, Blog blog2){
        LOGGER.info("判断博文是否相似：");
        //分词
        List<Word> blog1Words = WordSegmenter.seg(blog1.getTitle()+"\n"+blog1.getContent());
        List<Word> blog2Words = WordSegmenter.seg(blog2.getTitle()+"\n"+blog2.getContent());
        //词频统计
        Map<Word, AtomicInteger> blog1WordsFre = frequence(blog1Words);
        Map<Word, AtomicInteger> blog2WordsFre = frequence(blog2Words);
        //输出详细信息
        if(LOGGER.isDebugEnabled()){
            showDetail(blog1, blog1Words, blog1WordsFre);
            showDetail(blog2, blog2Words, blog2WordsFre);
        }
        //判断有几个相同的词
        AtomicInteger intersectionLength = new AtomicInteger();
        blog1WordsFre.keySet().forEach(word -> {
            if (blog2WordsFre.keySet().contains(word)) {
                intersectionLength.incrementAndGet();
            }
        });
        float threshold = Math.min(blog1WordsFre.size(), blog2WordsFre.size())*THRESHOLD_RATE;
        LOGGER.info("阈值=Math.min("+blog1WordsFre.size()+", "+blog2WordsFre.size()+")*"+THRESHOLD_RATE+"=" + threshold);
        LOGGER.info("博文1和2共有的词数：" + intersectionLength.get());
        if(intersectionLength.get() >= threshold){
            LOGGER.info("博文1和2共有的词数"+intersectionLength.get()+"大于或等于阈值：" + threshold);
            LOGGER.info("判断为相似");
            return true;
        }
        LOGGER.info("博文1和2共有的词数"+intersectionLength.get()+"小于阈值：" + threshold);
        LOGGER.info("判断为不相似");
        return false;
    }

    private void showDetail(Blog blog, List<Word> blogWords, Map<Word, AtomicInteger> blogWordsFre){
        LOGGER.debug("博文URL：");
        LOGGER.debug("\t"+blog.getUrl());
        LOGGER.debug("博文标题：");
        LOGGER.debug("\t"+blog.getTitle());
        LOGGER.debug("博文内容：");
        LOGGER.debug("\t"+blog.getContent());
        LOGGER.debug("博文长度："+blog.getContent().length());
        LOGGER.debug("博文分词结果：");
        LOGGER.debug("\t"+blogWords);
        LOGGER.debug("博文词频统计：");
        AtomicInteger c = new AtomicInteger();
        blogWordsFre
                .entrySet()
                .stream()
                .sorted((a,b)->b.getValue().get()-a.getValue().get())
                .forEach(e->LOGGER.debug("\t"+c.incrementAndGet()+"、"+e.getKey()+"="+e.getValue()));
    }

    private Map<Word, AtomicInteger> frequence(List<Word> words){
        Map<Word, AtomicInteger> fre =new HashMap<>();
        words.forEach(word->{
            fre.putIfAbsent(word, new AtomicInteger());
            fre.get(word).incrementAndGet();
        });
        return fre;
    }

    private Blog getBlog(String url) {
        try {
            String html = getHtml(url);
            Document doc = Jsoup.parse(html);
            Elements elements = doc.select(BLOG_CSS_PATH);
            String title = null;
            String content = null;
            for(Element element : elements){
                Elements ts = element.select(BLOG_TITLE_CSS_PATH);
                if(ts.size()==1){
                    title = ts.get(0).text();
                }
                ts = element.select(BLOG_CONTENT_CSS_PATH);
                if(ts.size()==1){
                    content = ts.get(0).text();
                }
            }
            if(title!=null && content!=null){
                Blog blog = new Blog();
                blog.setUrl(url);
                blog.setTitle(title);
                blog.setContent(content);
                return blog;
            }
        } catch (Exception e) {
            LOGGER.error("获取博文失败", e);
        }
        return null;
    }
    private String getHtml(String url){
        String html = getHtmlInternal(url);
        int times = 1;
        while (html==null && times<4){
            times++;
            //使用新的IP地址
            DynamicIp.toNewIp();
            html = getHtmlInternal(url);
        }
        times = 1;
        //LOGGER.debug("获取到的HTML：" +html);
        while((html.contains("非常抱歉，来自您ip的请求异常频繁")
                || html.contains("请您点击按钮解除封锁")
                || html.contains("请输入以下验证码"))
                && times<4){
            //使用新的IP地址
            DynamicIp.toNewIp();
            html = getHtmlInternal(url);
        }
        return html;
    }
    private String getHtmlInternal(String url) {
        try {
            return Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Referer", REFERER)
                    .header("Host", HOST)
                    .header("User-Agent", USER_AGENT)
                    .header("X-Forwarded-For", getRandomIp())
                    .header("Proxy-Client-IP", getRandomIp())
                    .header("WL-Proxy-Client-IP", getRandomIp())
                    .ignoreContentType(true)
                    .timeout(30000)
                    .get().html();
        } catch (Exception e) {
            LOGGER.error("获取博文失败", e);
        }
        return null;
    }
    private String getRandomIp(){
        int first = new Random().nextInt(254)+1;
        //排除A类私有地址0.0.0.0--10.255.255.255
        while(first==10){
            first = new Random().nextInt(254)+1;
        }
        int second = new Random().nextInt(254)+1;
        //排除B类私有地址172.16.0.0--172.31.255.255
        while(first==172 && (second>=16 && second<=31)){
            first = new Random().nextInt(254)+1;
            second = new Random().nextInt(254)+1;
        }
        //排除C类私有地址192.168.0.0--192.168.255.255
        while(first==192 && second==168){
            first = new Random().nextInt(254)+1;
            second = new Random().nextInt(254)+1;
        }
        int third = new Random().nextInt(254)+1;
        int forth = new Random().nextInt(254)+1;
        return first+"."+second+"."+second+"."+forth;
    }
    private static class Blog{
        private String url;
        private String title;
        private String content;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public static void main(String[] args) {
        SimilarChecker similarChecker = new ITEYEBlogSimilarChecker();
        similarChecker.isSimilar("http://baidu-27233181.iteye.com/blog/2200707",
                "http://baidu-27233181.iteye.com/blog/2200706");
    }
}
