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
import java.util.ArrayList;


/** Servlet that processes comments and analyses comment sentiment. */
@WebServlet("/data")
public class DataServlet extends HttpServlet {
  Gson gson;
  DatastoreService datastore;

  public DataServlet() {
    gson = new Gson();
    datastore = DatastoreServiceFactory.getDatastoreService();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {  
    // Get comments from Datastore and show on the page.
    ArrayList<Comment> comments = new ArrayList<Comment>();
    Query query = new Query("Comment").addSort("sentiment", SortDirection.DESCENDING);
    PreparedQuery results = datastore.prepare(query);

    for (Entity entity : results.asIterable()) {
        String comment_text = (String) entity.getProperty("text");
        double sentiment_score = (double) entity.getProperty("sentiment");
        Comment comment = new Comment(comment_text, sentiment_score);
        comments.add(comment);
    }

    String json = convertToJsonUsingGson(comments);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  /**
   * Converts a Java ArrayList<Comment> into a JSON string using the Gson library.
   */
  private String convertToJsonUsingGson(ArrayList<Comment> listOfStrings) {
    String json = gson.toJson(listOfStrings);
    return json;
  }

  /**
   * Class for comments.
   */
  private static class Comment {
      String text;
      double sentiment_score;

      public Comment(String text, double sentiment_score) {
        this.text = text;
        // truncate the double
        int temp = (int)(sentiment_score*100);
        double truncated = temp/100d;
        this.sentiment_score = truncated;
      }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Get the comment input from the form.
    String comment = getParameter(request, "text-input", "");

    // Calculate sentiment score.
    Document doc =
        Document.newBuilder().setContent(comment).setType(Document.Type.PLAIN_TEXT).build();
    LanguageServiceClient languageService = LanguageServiceClient.create();
    Sentiment sentiment = languageService.analyzeSentiment(doc).getDocumentSentiment();
    float score = sentiment.getScore();
    languageService.close();

    // Add the comment and its sentiment score to Datastore.
    if (!comment.equals("")) {
        Entity commentEntity = new Entity("Comment");
        commentEntity.setProperty("text", comment);
        commentEntity.setProperty("sentiment", score);
        datastore.put(commentEntity);
    }

    // Redirect back to the main page.
    response.sendRedirect("/index.html");
  }

  /**
   * @return the request parameter, or the default value if the parameter
   *         was not specified by the client
   */
  private String getParameter(HttpServletRequest request, String name, String defaultValue) {
    String value = request.getParameter(name);
    if (value == null) {
      return defaultValue;
    }
    return value;
  }
}
