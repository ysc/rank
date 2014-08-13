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

package org.seo.rank.api;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.seo.rank.Ranker;
import org.seo.rank.impl.BaiduRanker;
import org.seo.rank.list.Parser;
import org.seo.rank.list.impl.DefaultParser;
import org.seo.rank.model.Article;
import org.seo.rank.model.Rank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author 杨尚川
 */
@WebServlet(name = "GetListRank", urlPatterns = {"/GetListRank"})
public class GetListRank extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetListRank.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Parser PARSER = new DefaultParser();
    private static final Ranker RANKER = new BaiduRanker();

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        long start = System.currentTimeMillis();
        //获取栏目文章和链接
        String url = request.getParameter("url");
        String nextPageCssQuery = request.getParameter("nextPageCssQuery");
        String nextPageText = request.getParameter("nextPageText");
        String titleCssQuery = request.getParameter("titleCssQuery");
        String lastTime = request.getParameter("lastTime");
        String proxyHost = request.getParameter("proxyHost");
        String proxyPort = request.getParameter("proxyPort");
        LOGGER.info("url:"+url);
        LOGGER.info("nextPageCssQuery:"+nextPageCssQuery);
        LOGGER.info("nextPageText:"+nextPageText);
        LOGGER.info("titleCssQuery:"+titleCssQuery);
        List<Article> articles = PARSER.parse(url, nextPageCssQuery, nextPageText, titleCssQuery);
        LOGGER.info("文章数目："+articles.size());
        //将栏目文章和链接转换为排名对象
        List<Rank> ranks = new ArrayList<>();
        for(Article article : articles){
            Rank rank = new Rank();
            rank.setKeyword(article.getTitle());
            rank.setUrl(article.getUrl());
            ranks.add(rank);
        }
        //获取排名
        RANKER.rank(ranks);
        LOGGER.info("排名数目："+ranks.size());
        try (PrintWriter out = response.getWriter()) {
            String json = MAPPER.writeValueAsString(ranks);
            out.println(json);
        }
        long cost = System.currentTimeMillis() - start;
        LOGGER.info("GetListRank 耗时 "+cost+" 毫秒");
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
