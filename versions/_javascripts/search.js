function preInit() {
    indexes = {};
    axios.get('versions.json')
        .then(function(response) {
            let versions = response.data['versions'];
            let url = new URL(window.location.href);
            let searchText = url.searchParams.get('search-term');
            let version = url.searchParams.get('version');

            console.log(versions);
            let requests = [];
            let selectOptions = [];
            for (let i = 0; i < versions.length; i++) {
                console.log('data_' + versions[i]);
                requests.push(axios.get('data_' + versions[i] + '.json'))
                selectOptions.push(`<option value="${versions[i]}">${versions[i]}</option>`);
            }
            $('select').html(selectOptions);
            if (version) {
                $('select').val(version);
            }

            axios.all(requests)
                .then(function(responses) {
                    for (let i = 0; i < responses.length; i++) {
                        indexes[versions[i]] = buildIndex(responses[i].data);
                    }
                });
        })
        .catch(function(error) {
            console.log(error);
            alert("Something went wrong!");
        });
}

function buildIndex(data) {
    let keys = Object.keys(data[0]);
    let index = elasticlunr(function() {
        for (let i = 0; i < keys.length; i++) {
            this.addField(keys[i]);
        }
        this.setRef("topic_url");
    });
    for (let i = 0; i < data.length; i++) {
        index.addDoc(data[i]);
    }
    return index
}


let onSearch = function(searchText) {
    return new Promise(function(resolve, reject) {
        let activeVersion = $('select').val();
        let results = indexes[activeVersion].search(searchText, {
            extend: true
        });
        resolve(results);
    });
}

initPlugin(preInit, onSearch);