var quoteNum = 0;

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
  
  // Prevent the same quote from showing consecutively
  if (newQuoteNum == quoteNum) {
      ++newQuoteNum;
      newQuoteNum %= quotes.length;
  }
  quoteNum = newQuoteNum;
  const quote = quotes[quoteNum];

  // Add it to the page.
  const quoteContainer = document.getElementById('quote-container');
  quoteContainer.innerText = quote;
}
