const searchBox = document.getElementById('searchBox');
const suggestionsList = document.getElementById('suggestions');
const resultEl = document.getElementById('result');
const trendingEl = document.getElementById('trending');
const searchBtn = document.getElementById('searchBtn');
const refreshTrending = document.getElementById('refreshTrending');

let activeIndex = -1;
let suggestions = [];

function debounce(fn, wait = 250){
    let t; return (...args)=>{ clearTimeout(t); t = setTimeout(()=>fn(...args), wait); };
}

async function fetchSuggestions(q){
    if(!q) { renderSuggestions([]); return; }
    renderSuggestions([{loading:true}]);
    try{
        const res = await fetch(`/suggest?q=${encodeURIComponent(q)}`);
        const data = await res.json();
        suggestions = data;
        renderSuggestions(data);
    }catch(e){
        renderSuggestions([]);
    }
}

const debouncedFetch = debounce((q)=>fetchSuggestions(q),200);

searchBox.addEventListener('input', (e)=>{
    activeIndex = -1;
    debouncedFetch(e.target.value.trim());
});

searchBox.addEventListener('keydown', (e)=>{
    const items = suggestionsList.querySelectorAll('li');
    if(e.key === 'ArrowDown'){ e.preventDefault(); activeIndex = Math.min(activeIndex+1, items.length-1); updateActive(items); }
    else if(e.key === 'ArrowUp'){ e.preventDefault(); activeIndex = Math.max(activeIndex-1, 0); updateActive(items); }
    else if(e.key === 'Enter'){ e.preventDefault(); if(activeIndex>=0 && suggestions[activeIndex]) selectSuggestion(suggestions[activeIndex]); else submitSearch(); }
});

function updateActive(items){
    items.forEach((it,i)=> it.classList.toggle('active', i===activeIndex));
    if(activeIndex>=0 && items[activeIndex]) items[activeIndex].scrollIntoView({block:'nearest'});
}

function highlight(text, q){
    if(!q) return text;
    const re = new RegExp('('+escapeRegExp(q)+')','ig');
    return text.replace(re, '<mark>$1</mark>');
}

function escapeRegExp(s){ return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }

function renderSuggestions(list){
    suggestionsList.innerHTML = '';
    if(!list || list.length===0) return;
    if(list[0] && list[0].loading){
        const li = document.createElement('li'); li.textContent='Loading...'; suggestionsList.appendChild(li); return;
    }
    list.forEach((item, idx)=>{
        const li = document.createElement('li');
        li.innerHTML = `<div class="suggestion-text">${highlight(item.query, searchBox.value)}</div><div class="suggestion-meta">${item.totalCount||0}</div>`;
        li.addEventListener('click', ()=> selectSuggestion(item));
        li.addEventListener('mouseenter', ()=> { activeIndex = idx; updateActive(suggestionsList.querySelectorAll('li')); });
        suggestionsList.appendChild(li);
    });
}

function selectSuggestion(item){
    searchBox.value = item.query;
    suggestionsList.innerHTML = '';
    submitSearch();
}

async function submitSearch(){
    const query = searchBox.value.trim();
    if(!query) return;
    try{ await fetch(`/search?query=${encodeURIComponent(query)}`,{method:'POST'}); }
    catch(e){}
    resultEl.innerHTML = `You searched for <strong>${escapeHtml(query)}</strong>`;
    loadTrending();
}

function escapeHtml(s){ return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

async function loadTrending(){
    try{
        const res = await fetch('/trending');
        const data = await res.json();
        trendingEl.innerHTML='';
        data.forEach(t=>{
            const btn = document.createElement('button'); btn.className='chip'; btn.textContent=t; btn.addEventListener('click', ()=>{ searchBox.value=t; submitSearch(); });
            trendingEl.appendChild(btn);
        });
    }catch(e){ trendingEl.innerHTML=''; }
}

searchBtn.addEventListener('click', submitSearch);
refreshTrending && refreshTrending.addEventListener('click', loadTrending);

loadTrending();