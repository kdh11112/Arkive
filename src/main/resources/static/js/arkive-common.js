/* Arkive 공용 스크립트. jQuery 없이 동작한다. */

/* 테이블 행 클릭 하이라이트. tbody 행을 누르면 같은 tbody 안에서 선택 표시한다.
   빈 메시지 행(colspan)은 제외한다. 행별 동작(onclick 등과 함께 써도 된다. */
document.addEventListener('click', function (event) {
    var target = event.target instanceof Element ? event.target : null;
    if (!target) {
        return;
    }
    // POST 이동 링크는 하이라이트와 무관하게 먼저 처리하지 않는다(아래 별도 리스너가 담당).
    if (target.closest('a[data-post]')) {
        return;
    }
    var tr = target.closest('tbody tr');
    if (!tr) {
        return;
    }
    if (tr.querySelector('td[colspan]')) {
        return;
    }
    var tbody = tr.parentNode;
    tbody.querySelectorAll('tr.selected').forEach(function (row) {
        row.classList.remove('selected');
    });
    tr.classList.add('selected');
});

/* 아이디 저장. localStorage에 보관한다. 서버로 보내지 않는다. */
var SAVE_ID_KEY = 'arkiveSavedId';
function loadSavedId(inputId, checkId) {
    try {
        var saved = localStorage.getItem(SAVE_ID_KEY);
        if (saved) {
            document.getElementById(inputId).value = saved;
            document.getElementById(checkId).checked = true;
        }
    } catch (e) {}
}
function storeSavedId(userId, checked) {
    try {
        if (checked) {
            localStorage.setItem(SAVE_ID_KEY, userId);
        } else {
            localStorage.removeItem(SAVE_ID_KEY);
        }
    } catch (e) {}
}
/* 날짜·월·일시 입력은 칸을 눌러도 달력을 연다.
   type=date/month/datetime-local의 기본 동작은 달력 아이콘을 눌러야 열리므로,
   입력칸 어디를 눌러도 열리게 공통으로 처리한다. */
document.addEventListener('click', function (event) {
    var target = event.target instanceof Element ? event.target : null;
    if (!target) {
        return;
    }
    var input = target.closest('input[type="date"], input[type="month"], input[type="datetime-local"]');
    if (!input || input.disabled || input.readOnly || typeof input.showPicker !== 'function') {
        return;
    }
    try {
        input.showPicker();
    } catch (e) {
        // 구형 브라우저·이미 열린 달력은 브라우저 기본 동작을 따른다.
    }
});

/* POST 이동. URL 쿼리스트링 노출 없이 값을 넘긴다.
   a[data-post]를 누르면 href로 POST 폼을 만들어 보낸다.
   값은 data-name/data-value에 담는다. (th:attr="data-name='boardId',data-value=${item['boardId']}")
   data-p-xxx 방식은 쓰지 않는다. HTML 파서가 속성명을 소문자로 바꿔 boardId가 boardid가 되기 때문이다. */
document.addEventListener('click', function (event) {
    var target = event.target instanceof Element ? event.target : null;
    if (!target) {
        return;
    }
    var a = target.closest('a[data-post]');
    if (!a || !a.getAttribute('href')) {
        return;
    }
    var paramName = a.getAttribute('data-name');
    if (!paramName) {
        return;
    }
    event.preventDefault();
    var form = document.createElement('form');
    form.method = 'post';
    form.action = a.getAttribute('href');
    var input = document.createElement('input');
    input.type = 'hidden';
    input.name = paramName;
    input.value = a.getAttribute('data-value') || '';
    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
});
