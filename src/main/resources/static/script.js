const API_BASE = window.location.port === '5500' ? 'http://localhost:8080' : '';

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

async function fetchCacheDebug(q){
    const panel = document.getElementById('cacheDebugPanel');
    const nodeEl = document.getElementById('debugNode');
    const statusEl = document.getElementById('debugStatus');
    if(!q) { panel.style.display = 'none'; return; }
    try{
        const res = await fetch(`${API_BASE}/cache/debug?prefix=${encodeURIComponent(q)}`);
        const data = await res.json();
        nodeEl.textContent = data.responsibleNode || 'None';
        if(data.isHit){
            statusEl.textContent = 'HIT';
            statusEl.className = 'badge badge-hit';
        } else {
            statusEl.textContent = 'MISS';
            statusEl.className = 'badge badge-miss';
        }
        panel.style.display = 'flex';
    }catch(e){
        panel.style.display = 'none';
    }
}

async function fetchSuggestions(q){
    if(!q) { 
        renderSuggestions([]); 
        document.getElementById('cacheDebugPanel').style.display = 'none';
        return; 
    }
    renderSuggestions([{loading:true}]);
    fetchCacheDebug(q);
    try{
        const mode = document.querySelector('input[name="rankingMode"]:checked').value;
        const res = await fetch(`${API_BASE}/suggest?q=${encodeURIComponent(q)}&mode=${mode}`);
        const data = await res.json();
        suggestions = data;
        renderSuggestions(data);
        setTimeout(() => fetchCacheDebug(q), 100);
    }catch(e){
        renderSuggestions([]);
    }
}

document.querySelectorAll('input[name="rankingMode"]').forEach(radio => {
    radio.addEventListener('change', () => {
        const q = searchBox.value.trim();
        if(q) fetchSuggestions(q);
    });
});

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
    try{ await fetch(`${API_BASE}/search?query=${encodeURIComponent(query)}`,{method:'POST'}); }
    catch(e){}
    resultEl.innerHTML = `You searched for <strong>${escapeHtml(query)}</strong>`;
    loadTrending();
}

function escapeHtml(s){ return s.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;'); }

async function loadTrending(){
    try{
        const res = await fetch(`${API_BASE}/trending`);
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