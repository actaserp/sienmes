// inputFormat.js

function attachTelFormatter(inputSelector) {
    const telInput = document.querySelector(inputSelector);
    if (!telInput) return;

    telInput.addEventListener('input', function(e) {
        let numbers = e.target.value.replace(/[^0-9]/g, '');
        if (numbers.length > 11) numbers = numbers.slice(0, 11);

        let result = '';
        if(numbers.length < 4) {
            result = numbers;
        } else if(numbers.length < 7) {
            result = numbers.slice(0, 3) + '-' + numbers.slice(3);
        } else if(numbers.length === 10) {
            result = numbers.slice(0, 3) + '-' + numbers.slice(3, 6) + '-' + numbers.slice(6, 10);
        } else if(numbers.length === 11) {
            result = numbers.slice(0, 3) + '-' + numbers.slice(3, 7) + '-' + numbers.slice(7, 11);
        } else {
            result = numbers.slice(0, 2) + '-' + numbers.slice(2, 5) + '-' + numbers.slice(5);
        }
        e.target.value = result;
    });
}

// 전역 등록
window.attachTelFormatter = attachTelFormatter;
