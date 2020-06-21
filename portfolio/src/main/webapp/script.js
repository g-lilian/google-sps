var prevQuoteNum = 0;

/** Adds a random quote to the page. */
function addRandomQuote() {
  const quotes =
      ['“Art washes away from the soul the dust of everyday life.” --Pablo Picasso',
       '“Fiction, because it is not about somebody who actually lived in the real world, always has the possibility of being about oneself.” --Orson Scott Card',
       '“Somewhere, something incredible is waiting to be known.” --Carl Sagan',
       '“Imagination is more important than knowledge. Knowledge is limited. Imagination encircles the world.” --Albert Einstein',
       '“Any sufficiently advanced technology is indistinguishable from magic.” --Arthur C. Clarke'];

  // Pick a random quote.
  var newQuoteNum = Math.floor(Math.random() * quotes.length);
  
  // If the quote is the same as the prev one generated, pick the next one in the array.
  if (newQuoteNum == prevQuoteNum) {
      ++newQuoteNum;
      newQuoteNum %= quotes.length;
  }

  // Add it to the page.
  const quoteContainer = document.getElementById('quote-container');
  quoteContainer.innerText = quotes[newQuoteNum];
  prevQuoteNum = newQuoteNum;
}

/** Fetch comments from backend and display in sections. */
async function getComments() {
  const response = await fetch('/data');
  var commentsList = await response.text();

  // Display comments in sections.
  commentsList = JSON.parse(commentsList)
  for (i=0; i<commentsList.length; i++) {
    var commentsContainer = document.getElementById('comments-container')
    const comment = commentsList[i];
    var commentSection = createCommentSection(comment);
    commentsContainer.appendChild(commentSection);
  }
}

function createCommentSection(comment) {
  const commentWrapper = document.createElement("div");
  commentWrapper.id = "comment-wrapper";

  const sentimentScore = comment.sentimentScore.toFixed(2);
  commentWrapper.innerHTML = 
    `<div class="row" style="background-color:gainsboro; color:black;">
      <div class="col-left"><p>${comment.alias}</p></div>
      <div class="col-right"><p>${comment.timestamp}</p></div>
    </div>
    <div class="row">
      <div class="col-left"><p>${comment.text}</p></div>
      <div class="col-right"><p>${sentimentScore}</p></div>
    </div><br>`;
  return commentWrapper;
}

google.charts.load('current', {'packages':['corechart']});
google.charts.setOnLoadCallback(drawChart);

/** Create sentiment score piechart and add it to the page. */
async function drawChart() {
  const response = await fetch('/data');
  var commentsList = await response.text();
  commentsList = JSON.parse(commentsList)
  var pos = neg = neut = 0;
  for (i=0; i<commentsList.length; i++) {
    const comment = commentsList[i];
    const sentimentScore = comment.sentimentScore;
    if (sentimentScore > 0.3) ++pos;
    else if (sentimentScore < -0.3) ++neg;
    else ++neut;
  }

  const data = new google.visualization.DataTable();
  data.addColumn('string', 'Sentiment');
  data.addColumn('number', 'Count');
  data.addRows([
    ['Positive (>0.3)', pos],
    ['Neutral', neut],
    ['Negative (<-0.3)', neg]
  ]);

  const options = {
    'title': 'How people are feeling',
    'width':600,
    'height':400,
    'colors':['#FFC1C1','#AAD1A8','#A8B9D1'],
    backgroundColor: 'transparent',
    'is3D':true
  };

  const chart = new google.visualization.PieChart(
      document.getElementById('chart-container'));
  chart.draw(data, options);
}
