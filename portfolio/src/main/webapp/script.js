var prevQuoteNum = 0;

/**
 * Adds a random quote to the page.
 */
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

async function getComments() {
  const response = await fetch('/data');
  var commentsList = await response.text();

  // Display comments in a list.
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
  commentWrapper.innerHTML = 
    `<div class="row">
      <div class="column">
        <p>${comment.text}</p>
      </div>
      <div class="column">
        <p>${comment.sentiment_score}</p>
      </div>
    </div><hr>`;
  return commentWrapper;
}
