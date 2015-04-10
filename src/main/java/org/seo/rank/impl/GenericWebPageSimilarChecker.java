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
import org.seo.rank.SimilarChecker;
import org.seo.rank.list.DynamicIp;
import org.seo.rank.list.impl.DefaultParser;
import org.seo.rank.model.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 通用的网页相似度检测算法
 * @author 杨尚川
 */
public class GenericWebPageSimilarChecker implements SimilarChecker{
    private static final Logger LOGGER = LoggerFactory.getLogger(GenericWebPageSimilarChecker.class);
    private static final String ACCEPT = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    private static final String ENCODING = "gzip, deflate";
    private static final String LANGUAGE = "zh-cn,zh;q=0.8,en-us;q=0.5,en;q=0.3";
    private static final String CONNECTION = "keep-alive";
    private static final String REFERER = "http://www.baidu.com";
    private static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.10; rv:36.0) Gecko/20100101 Firefox/36.0";
    private static final float THRESHOLD_RATE = 0.5F;

    @Override
    public boolean isSimilar(String url1, String url2) {
        return similarScore(url1, url2)>=THRESHOLD_RATE;
    }
    @Override
    public double similarScore(String url1, String url2) {
        WebPage webPage1 = getWebPage(url1);
        if(webPage1!=null) {
            WebPage webPage2 = getWebPage(url2);
            if(webPage2!=null) {
                double score = score(webPage1, webPage2);
                //取两位小数
                score = (int)(score*100)/(double)100;
                return score;
            }
        }
        return 0;
    }

    private double score(WebPage webPage1, WebPage webPage2){
        //分词
        List<Word> webPage1Words = WordSegmenter.seg(webPage1.getTitle()+"\n"+webPage1.getContent());
        List<Word> webPage2Words = WordSegmenter.seg(webPage2.getTitle()+"\n"+webPage2.getContent());
        //词频统计
        Map<Word, AtomicInteger> webPage1WordsFre = frequence(webPage1Words);
        Map<Word, AtomicInteger> webPage2WordsFre = frequence(webPage2Words);
        //输出详细信息
        if(LOGGER.isDebugEnabled()){
            showDetail(webPage1, webPage1Words, webPage1WordsFre);
            showDetail(webPage2, webPage2Words, webPage2WordsFre);
        }
        //使用简单共有词判定
        //return simpleScore(webPage1WordsFre, webPage2WordsFre);
        //使用余弦相似度判定
        return cosScore(webPage1WordsFre, webPage2WordsFre);
    }

    /**
     * 判定相似性的方式一：简单共有词
     * @param webPage1WordsFre
     * @param webPage2WordsFre
     * @return
     */
    private double simpleScore(Map<Word, AtomicInteger> webPage1WordsFre, Map<Word, AtomicInteger> webPage2WordsFre){
        //判断有几个相同的词
        AtomicInteger intersectionLength = new AtomicInteger();
        webPage1WordsFre.keySet().forEach(word -> {
            if (webPage2WordsFre.keySet().contains(word)) {
                intersectionLength.incrementAndGet();
            }
        });
        LOGGER.info("网页1有的词数：" + webPage1WordsFre.size());
        LOGGER.info("网页2有的词数：" + webPage2WordsFre.size());
        LOGGER.info("网页1和2共有的词数：" + intersectionLength.get());
        double score = intersectionLength.get()/Math.min(webPage1WordsFre.size(), webPage2WordsFre.size());
        LOGGER.info("相似度分值="+intersectionLength.get()+"/Math.min("+webPage1WordsFre.size()+", "+webPage2WordsFre.size()+")="+score);
        return score;
    }

    /**
     *
     * 判定相似性的方式二：余弦相似度
     * 余弦夹角原理：
     * 向量a=(x1,y1),向量b=(x2,y2)
     * a.b=x1x2+y1y2
     * |a|=根号[(x1)^2+(y1)^2],|b|=根号[(x2)^2+(y2)^2]
     * a,b的夹角的余弦cos=a.b/|a|*|b|=(x1x2+y1y2)/根号[(x1)^2+(y1)^2]*根号[(x2)^2+(y2)^2]
     * @param webPage1WordsFre
     * @param webPage2WordsFre
     */
    private double cosScore(Map<Word, AtomicInteger> webPage1WordsFre, Map<Word, AtomicInteger> webPage2WordsFre){
        Set<Word> words = new HashSet<>();
        words.addAll(webPage1WordsFre.keySet());
        words.addAll(webPage2WordsFre.keySet());
        //向量的维度为words的大小，每一个维度的权重是词频，注意的是，中文分词的时候已经去了停用词
        //a.b
        AtomicInteger ab = new AtomicInteger();
        //|a|
        AtomicInteger aa = new AtomicInteger();
        //|b|
        AtomicInteger bb = new AtomicInteger();
        //计算
        words
            .stream()
            .forEach(word -> {
                AtomicInteger x1 = webPage1WordsFre.get(word);
                AtomicInteger x2 = webPage2WordsFre.get(word);
                if(x1!=null && x2!=null) {
                    //x1x2
                    int oneOfTheDimension = x1.get() * x2.get();
                    //+
                    ab.addAndGet(oneOfTheDimension);
                }
                if(x1!=null){
                    //(x1)^2
                    int oneOfTheDimension = x1.get() * x1.get();
                    //+
                    aa.addAndGet(oneOfTheDimension);
                }
                if(x2!=null){
                    //(x2)^2
                    int oneOfTheDimension = x2.get() * x2.get();
                    //+
                    bb.addAndGet(oneOfTheDimension);
                }
            });

        double aaa = Math.sqrt(aa.get());
        double bbb = Math.sqrt(bb.get());
        //使用BigDecimal保证精确计算浮点数
        BigDecimal aabb = BigDecimal.valueOf(aaa).multiply(BigDecimal.valueOf(bbb));
        double cos = ab.get()/aabb.doubleValue();
        return cos;
    }

    private void showDetail(WebPage webPage, List<Word> webPageWords, Map<Word, AtomicInteger> webPageWordsFre){
        LOGGER.debug("网页URL：");
        LOGGER.debug("\t"+webPage.getUrl());
        LOGGER.debug("网页标题：");
        LOGGER.debug("\t"+webPage.getTitle());
        LOGGER.debug("网页内容：");
        LOGGER.debug("\t"+webPage.getContent());
        LOGGER.debug("网页长度："+webPage.getContent().length());
        LOGGER.debug("网页分词结果：");
        LOGGER.debug("\t"+webPageWords);
        LOGGER.debug("网页词频统计：");
        AtomicInteger c = new AtomicInteger();
        webPageWordsFre
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

    private WebPage getWebPage(String url){
        WebPage webPage = getWebPageInternal(url);
        int times = 1;
        while (webPage==null && times<4){
            times++;
            //使用新的IP地址
            DynamicIp.toNewIp();
            webPage = getWebPageInternal(url);
        }
        if(webPage==null){
            return null;
        }
        times = 1;
        //LOGGER.debug("获取到的HTML：" +html);
        while((webPage.getContent().contains("非常抱歉，来自您ip的请求异常频繁")
                || webPage.getContent().contains("请您点击按钮解除封锁")
                || webPage.getContent().contains("请输入以下验证码"))
                && times<4){
            times++;
            //使用新的IP地址
            DynamicIp.toNewIp();
            webPage = getWebPageInternal(url);
        }
        return webPage;
    }
    private WebPage getWebPageInternal(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .header("Accept", ACCEPT)
                    .header("Accept-Encoding", ENCODING)
                    .header("Accept-Language", LANGUAGE)
                    .header("Connection", CONNECTION)
                    .header("Referer", "http://"+new URL(url).getHost())
                    .header("Host", new URL(url).getHost())
                    .header("User-Agent", USER_AGENT)
                    .header("X-Forwarded-For", getRandomIp())
                    .header("Proxy-Client-IP", getRandomIp())
                    .header("WL-Proxy-Client-IP", getRandomIp())
                    .ignoreContentType(true)
                    .timeout(30000)
                    .get();
            WebPage webPage = new WebPage();
            webPage.setUrl(url);
            webPage.setContent(doc.text());
            webPage.setTitle(doc.title());
            return webPage;
        } catch (Exception e) {
            LOGGER.error("获取网页失败", e);
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
    private static class WebPage{
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

    /**
     * 我的ITEYE和OSCHINA博客有很多同样的博文，主要目的是备份
     * 这里刚好用来测试相似性检测算法的效果
     * http://yangshangchuan.iteye.com
     * http://my.oschina.net/apdplat/blog
     */
    private void verifyYscBlog(){
        List<Article> ob = DefaultParser.oschinaBlog();
        List<Article> ib = DefaultParser.iteyeBlog();
        Map<String, String> om = new HashMap<>();
        Map<String, String> im = new HashMap<>();
        ob.stream().forEach(b->om.put(b.getTitle(), b.getUrl()));
        ib.stream().forEach(b->im.put(b.getTitle(), b.getUrl()));
        List<String> oschinaBlog = ob.stream().map(b -> b.getTitle()).sorted().collect(Collectors.toList());
        List<String> iteyeBlog   = ib.stream().map(b -> b.getTitle()).sorted().collect(Collectors.toList());

        List<String> commons = oschinaBlog.stream().filter(b -> iteyeBlog.contains(b)).collect(Collectors.toList());
        commons.remove("自动更改IP地址反爬虫封锁，支持多线程");
        Map<String, Double> result = new HashMap<>();
        AtomicInteger similarCount = new AtomicInteger();
        commons.forEach(title -> {
            double score = similarScore(om.get(title), im.get(title));
            result.put(title, score);
            if (score >= THRESHOLD_RATE) {
                similarCount.incrementAndGet();
            }
        });
        LOGGER.info("<h4>检查的博文数：" + commons.size() + "，相似度大于等于" + THRESHOLD_RATE + "的博文数：" + similarCount.get() + "，相似度小于" + THRESHOLD_RATE + "的博文数：" + (commons.size() - similarCount.get())+"</h4>");
        AtomicInteger i = new AtomicInteger();
        result
            .entrySet()
            .stream()
            .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
            .forEach(e -> {
                LOGGER.info("");
                LOGGER.info("<h4>"+i.incrementAndGet() + "、检查博文" + "：" + e.getKey()+"，相似度分值："+e.getValue().doubleValue()+"</h4>");
            LOGGER.info("\t博文地址1：<a target=\"_blank\" href=\""+om.get(e.getKey())+"\">"+om.get(e.getKey())+"</a><br/>");
            LOGGER.info("\t博文地址2：<a target=\"_blank\" href=\""+im.get(e.getKey())+"\">"+im.get(e.getKey())+"</a><br/>");
            });
    }
    public static void main(String[] args) throws Exception{
        GenericWebPageSimilarChecker genericWebPageSimilarChecker = new GenericWebPageSimilarChecker();
        genericWebPageSimilarChecker.verifyYscBlog();
    }
}
