/* Arkive 공용 스크립트. jQuery 없이 동작한다. */

/* 테이블 행 클릭 하이라이트. tbody 행을 누르면 같은 tbody 안에서 선택 표시한다.
   빈 메시지 행(colspan)은 제외한다. 행별 동작(onclick 등)과 함께 써도 된다. */
document.addEventListener('click', function (event) {
    var target = event.target instanceof Element ? event.target : null;
    if (!target) {
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
