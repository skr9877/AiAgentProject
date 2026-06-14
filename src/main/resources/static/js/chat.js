const sessionId = window.SESSION_ID;
const userId    = window.USER_ID;
const ws = new WebSocket(`ws://${location.host}/ws/${sessionId}?userId=${encodeURIComponent(userId)}`);

// _header.html: id="statusText" — 연결 상태 텍스트 표시
const statusText = document.getElementById('statusText');

// _body.html: id="messages" — 채팅 메시지 목록 컨테이너
const messages   = document.getElementById('messages');

// _body.html: id="input" — 메시지 입력 텍스트 필드
const input      = document.getElementById('input');

// _body.html: id="sendBtn" — 메시지 전송 버튼 (SVG 아이콘)
const sendBtn    = document.getElementById('sendBtn');

// _body.html: id="typingRow" — AI 응답 대기 중 타이핑 인디케이터 행
const typingRow  = document.getElementById('typingRow');

// _body.html: id="categoryBar" — 카테고리 버튼들을 담는 가로 스크롤 바
const categoryBar = document.getElementById('categoryBar');

// _body.html: id="catLeft" — 카테고리 바 왼쪽 스크롤 버튼 (‹)
const catLeft    = document.getElementById('catLeft');

// _body.html: id="catRight" — 카테고리 바 오른쪽 스크롤 버튼 (›)
const catRight   = document.getElementById('catRight');

const selectedCategories = new Set();

// _body.html: id="catLeft" / id="catRight" — 카테고리 바 좌우 스크롤 처리
catLeft.addEventListener('click',  () => { categoryBar.scrollLeft -= 80; });
catRight.addEventListener('click', () => { categoryBar.scrollLeft += 80; });

// _body.html: .category-btn (data-category="주문조회" | "유저조회") — 카테고리 선택/해제 토글
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

// _body.html: id="messages" — 채팅창을 항상 최신 메시지로 스크롤
function scrollToBottom() {
    messages.scrollTop = messages.scrollHeight;
}

// AI 응답 텍스트에서 <IMG>파일명<IMG> 태그를 <img> 엘리먼트로 변환
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

// XSS 방지용 HTML 이스케이프
function escapeHtml(str) {
    return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// _body.html: id="messages" — 메시지 버블 생성 후 채팅창에 추가 (type: 'user' | 'ai' | 'system')
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

// _body.html: id="typingRow" — AI 응답 대기 중 타이핑 인디케이터 표시
function showTyping() {
    typingRow.style.display = 'block';
    scrollToBottom();
}

// _body.html: id="typingRow" — 타이핑 인디케이터 숨김
function hideTyping() {
    typingRow.style.display = 'none';
}

// _header.html: id="statusText" / _body.html: id="input", id="sendBtn" — WebSocket 연결 성공 시 UI 활성화
ws.onopen = () => {
    statusText.textContent = '연결됨';
    statusText.style.color = '#a5f3c8';
    input.disabled = false;
    sendBtn.disabled = false;
    input.focus();
};

// _body.html: id="messages", id="typingRow" — 서버 메시지 수신 시 말풍선 렌더링
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

// _header.html: id="statusText" / _body.html: id="input", id="sendBtn" — WebSocket 연결 종료 시 UI 비활성화
ws.onclose = () => {
    statusText.textContent = '연결 종료';
    statusText.style.color = '#fca5a5';
    input.disabled = true;
    sendBtn.disabled = true;
    hideTyping();
};

// _header.html: id="statusText" — WebSocket 오류 발생 시 상태 표시
ws.onerror = () => {
    statusText.textContent = '연결 오류';
    statusText.style.color = '#fca5a5';
};

// _body.html: id="input", id="sendBtn" — 입력창 텍스트와 선택된 카테고리를 WebSocket으로 전송
function sendMessage() {
    const text = input.value.trim();
    if (!text || sendBtn.disabled || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ text, categories: [...selectedCategories] }));
    input.value = '';
    showTyping();
}

// _body.html: id="sendBtn" — 전송 버튼 클릭 이벤트
sendBtn.addEventListener('click', sendMessage);

// _body.html: id="input" — Enter 키 입력 시 메시지 전송
input.addEventListener('keydown', (e) => {
    if (e.key === 'Enter') sendMessage();
});
