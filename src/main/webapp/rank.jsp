<%--
   APDPlat - Application Product Development Platform
   Copyright (c) 2013, 杨尚川, yang-shangchuan@qq.com
   
   This program is free software: you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation, either version 3 of the License, or
   (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>获取关键词和链接的百度排名API调用演示</title>
    </head>
    <body>
        <h2><font color="blue">获取关键词和链接的百度排名API调用演示</font></h2>
        
        <form action="GetRank" method="post">
            URL地址：<input name="url" size="150" maxlength="150"><br/>
            关键词：<input name="keyword" size="150" maxlength="150"><br/>
            <input type="submit" value="获取关键词和链接的百度排名"/>
        </form>
    </body>
</html>