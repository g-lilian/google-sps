// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;
import com.google.gson.Gson;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.Sentiment;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.ArrayList;


/** Servlet that processes comments and analyses comment sentiment. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  Gson gson;
  DatastoreService datastore;
  LanguageServiceClient languageService;

  public DataServlet() throws IOException {
    gson = new Gson();
    datastore = DatastoreServiceFactory.getDatastoreService();
    languageService = LanguageServiceClient.create();
  }

  /**
   * Class for comments.
   */
  private static class Comment {
      String text;
      double sentimentScore;
      String timestamp;
      String alias;

      public Comment(String text, double sentimentScore, String timestamp, String alias) {
        this.text = text;
        this.sentimentScore = sentimentScore;
        this.timestamp = timestamp;
        this.alias = alias;
      }
  }

  /** Get comments from Datastore and send to script.js. */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {  
    // Get comments from Datastore and show on the page.
    ArrayList<Comment> comments = new ArrayList<Comment>();
    Query query = new Query("Comment").addSort("sentiment", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
        String commentText = (String) entity.getProperty("text");
        double sentimentScore = (double) entity.getProperty("sentiment");
        String timestamp = (String) entity.getProperty("timestamp");
        String alias = (String) entity.getProperty("alias");
        Comment comment = new Comment(commentText, sentimentScore, timestamp, alias);
        comments.add(comment);
    }

    String json = convertToJsonUsingGson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Converts a Java ArrayList<Comment> into a JSON string using the Gson library.
   */
  private String convertToJsonUsingGson(ArrayList<Comment> commentsList) {
    String json = gson.toJson(commentsList);
    return json;
  }

  /** Get comments from form, calculate sentiment score and put in Datastore. */
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the comment input from the form.
    String comment = getParameter(request, "text-input", "");
    String alias = getParameter(request, "name-input", "anonymous");

    // Get current timestamp for time when comment is posted.
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    String timestamp = dtf.format(now);

    // Calculate sentiment score.
    Document doc =
        Document.newBuilder().setContent(comment).setType(Document.Type.PLAIN_TEXT).build();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    float score = sentiment.getScore();

    // Add the comment and its sentiment score to Datastore.
    Entity commentEntity = new Entity("Comment");
    commentEntity.setProperty("text", comment);
    commentEntity.setProperty("sentiment", score);
    commentEntity.setProperty("timestamp", timestamp);
    commentEntity.setProperty("alias", alias);
    datastore.put(commentEntity);

    // Redirect back to the main page.
    response.sendRedirect("/index.html");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value.isEmpty()) {
      return defaultValue;
    }
    return value;
  }
}
