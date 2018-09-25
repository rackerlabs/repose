let initPlugin = function(onInit, onSearch, searchBox = '#searchBox', versionsDropDown = 'select', resultsWrapper = '.result-wrapper', paginationWrapper = '.pagination-wrapper') {
    let stopWords = { "": true, "a": true, "able": true, "about": true, "across": true, "after": true, "all": true, "almost": true, "also": true, "am": true, "among": true, "an": true, "and": true, "any": true, "are": true, "as": true, "at": true, "be": true, "because": true, "been": true, "but": true, "by": true, "can": true, "cannot": true, "could": true, "dear": true, "did": true, "do": true, "does": true, "either": true, "else": true, "ever": true, "every": true, "for": true, "from": true, "get": true, "got": true, "had": true, "has": true, "have": true, "he": true, "her": true, "hers": true, "him": true, "his": true, "how": true, "however": true, "i": true, "if": true, "in": true, "into": true, "is": true, "it": true, "its": true, "just": true, "least": true, "let": true, "like": true, "likely": true, "may": true, "me": true, "might": true, "most": true, "must": true, "my": true, "neither": true, "no": true, "nor": true, "not": true, "of": true, "off": true, "often": true, "on": true, "only": true, "or": true, "other": true, "our": true, "own": true, "rather": true, "said": true, "say": true, "says": true, "she": true, "should": true, "since": true, "so": true, "some": true, "than": true, "that": true, "the": true, "their": true, "them": true, "then": true, "there": true, "these": true, "they": true, "this": true, "tis": true, "to": true, "too": true, "twas": true, "us": true, "wants": true, "was": true, "we": true, "were": true, "what": true, "when": true, "where": true, "which": true, "while": true, "who": true, "whom": true, "why": true, "will": true, "with": true, "would": true, "yet": true, "you": true, "your": true };
    if (onInit) {
        onInit(searchBox, versionsDropDown, resultsWrapper);
    }
    let searchText = '';
    $(searchBox).change(function() {
        searchText = $(searchBox).val();
        onSearch(searchText).then(function(results) {
            results = highlightAndFormatResults(results);
            paginateResults(results);
        });
    });

    $(versionsDropDown).change(function() {
        searchText = $(searchBox).val();
        onSearch(searchText).then(function(results) {
            results = highlightAndFormatResults(results);
            paginateResults(results);
        });
    });

    let paginateResults = function(results) {
        $(paginationWrapper + ' > span').html('Total Results: ' + results.length);
        $('.result-wrapper > .pagination-wrapper').pagination({
            dataSource: results,
            pageSize: 10,
            callback: function(data, pagination) {
                console.log(data.length);
                var container = $('.result-wrapper > .results');
                container.html('');
                for (var i = 0; i < data.length; i++) {
                    container.append(data[i]);
                }
            }
        });
    };

    let highlightAndFormatResults = function(results) {
        let text = searchText;
        let words = text.split(' ').filter(function(value) {
            return stopWords[value] !== null;
        });
        let finalResults = [];
        for (let i = 0; i < results.length; i++) {
            for (let j = 0; j < words.length; j++) {
                let line = '';
                let keywordPos = results[i].doc.content.toLowerCase().indexOf(words[j].toLowerCase());
                if (keywordPos > -1) {
                    line += ' ' + results[i].doc.content.slice(keywordPos, keywordPos + 240);
                } else {
                    line += ' ' + results[i].doc.content.slice(0, 240);
                }
                line += '...';
                let re = new RegExp(words[j], 'i');
                line = line.replace(re, `<b>${words[j]}</b>`);
                let html = `<div class="row">
                        <a href="${results[i].doc.topic_url}">${results[i].doc.title}</a>
                        <span>${results[i].doc.site_name + results[i].doc.topic_url}</span>
                        <p>
                            ${line}
                        </p>
                    </div>`;
                //resultsMarkup += html
                finalResults.push(html);
            }
        }
        return finalResults;
    };

};