const sessionId = window.SESSION_ID;
const ws = new WebSocket(`ws://${location.host}/ws/${sessionId}`);

const messages   = document.getElementById('messages');
const input      = document.getElementById('input');
const sendBtn    = document.getElementById('sendBtn');
const statusText = document.getElementById('statusText');
const typingRow  = document.getElementById('typingRow');
const categoryBar = document.getElementById('categoryBar');
const catLeft    = document.getElementById('catLeft');
const catRight   = document.getElementById('catRight');

const selectedCategories = new Set();

catLeft.addEventListener('click',  () => { categoryBar.scrollLeft -= 80; });
catRight.addEventListener('click', () => { categoryBar.scrollLeft += 80; });

document.querySelectorAll('.category-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const cat = btn.dataset.category;
        if (selectedCategories.has(cat)) {
            selectedCategories.delete(cat);
            btn.classList.remove('active');
        } else {
            selectedCategories.add(cat);
            btn.classList.add('active');
        }
    });
});

function scrollToBottom() {
    messages.scrollTop = messages.scrollHeight;
}

function renderContent(text) {
    const imgRegex = /<IMG>(.*?)<IMG>/gi;
    const parts = [];
    let last = 0, match;
    while ((match = imgRegex.exec(text)) !== null) {
        if (match.index > last) {
            parts.push(escapeHtml(text.slice(last, match.index)).replace(/\n/g, '<br>'));
        }
        const name = match[1].trim();
        parts.push(`<img src="/assets/images/${escapeHtml(name)}" alt="${escapeHtml(name)}" style="max-width:100%;border-radius:8px;margin-top:6px;display:block;">`);
        last = match.index + match[0].length;
    }
    if (last < text.length) {
        parts.push(escapeHtml(text.slice(last)).replace(/\n/g, '<br>'));
    }
    return parts.join('');
}

function escapeHtml(str) {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function addMessage(text, type) {
    const row = document.createElement('div');
    if (type === 'system') {
        row.className = 'system-msg';
        row.textContent = text;
    } else {
        row.className = `msg-row ${type}`;
        const bubble = document.createElement('div');
        bubble.className = 'bubble';
        if (type === 'ai') {
            bubble.innerHTML = renderContent(text);
        } else {
            bubble.textContent = text;
        }
        row.appendChild(bubble);
    }
    messages.appendChild(row);
    scrollToBottom();
}

function showTyping() {
    typingRow.style.display = 'block';
    scrollToBottom();
}

function hideTyping() {
    typingRow.style.display = 'none';
}

ws.onopen = () => {
    statusText.textContent = '연결됨';
    statusText.style.color = '#a5f3c8';
    input.disabled = false;
    sendBtn.disabled = false;
    input.focus();
};

ws.onmessage = (e) => {
    hideTyping();
    const text = e.data;
    if (text.startsWith('SYSTEM:')) {
        addMessage(text.replace('SYSTEM:', '').trim(), 'system');
    } else if (text.startsWith('고객: ')) {
        addMessage(text.slice('고객: '.length), 'user');
    } else if (text.startsWith('AI: ')) {
        addMessage(text.slice('AI: '.length), 'ai');
    }
};

ws.onclose = () => {
    statusText.textContent = '연결 종료';
    statusText.style.color = '#fca5a5';
    input.disabled = true;
    sendBtn.disabled = true;
    hideTyping();
};

ws.onerror = () => {
    statusText.textContent = '연결 오류';
    statusText.style.color = '#fca5a5';
};

function sendMessage() {
    const text = input.value.trim();
    if (!text || sendBtn.disabled || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ text, categories: [...selectedCategories] }));
    input.value = '';
    showTyping();
}

sendBtn.addEventListener('click', sendMessage);
input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendMessage();
});
