function submitTextarea(event, func){
    let key = event.key || event.keyCode;

    if(key === 'Enter' || key == 13){
        func();
    }
}

//오늘낧짜
function getNowDate(){
    const today = new Date();

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`
    }
    return formattedDate(today);
}
// 7일전 날짜
function getNowDateMinus7(){

    const day = new Date();

    const sevenDaysAgo = new Date();

    sevenDaysAgo.setDate(day.getDate() - 7);

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0')
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    return formattedDate(sevenDaysAgo);
}

//현재 시간, HH:mm
function getNowTime(){
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`
}

function getFirstDayOfThreeMonthsAgo(){
    const today = new Date();

    const threeMonthsAgo = new Date(today.getFullYear(), today.getMonth() - 3, 1);

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    return formattedDate(threeMonthsAgo);
}

// 3달이상이면 false
function calculateDay(fd, td){



    const frdate = new Date(fd);
    const todate = new Date(td);

    const diffInMs = Math.abs(frdate - todate);
    const diffDays = diffInMs / (1000 * 60 * 60 * 24);

    return diffDays <= 90;
}

function validationCheck(FormData, checklist){

    for(let key in checklist){
        const value = FormData[key];

        if(value === null || value === ''){
            const text = checklist[key];

            Alert.alert('', `${text} 항목이 빈 값입니다.`);
            return false;
        }
    }
    return true
}

function ApiSuccessMessage(res){
    console.log(res);
    console.log('func', typeof func);
    if(res === null || res === undefined){
        Alert.alert('', '에러가 발생하였습니다.');
    }
    Alert.alert('', res.message);
}

function selectBoxBinding(id, arr){

    const selectedBox = document.getElementById(id);

    arr.forEach(item => {
        const option = document.createElement('option');
        option.value = item.value;
        option.textContent = item.text;
        selectedBox.appendChild(option);
    })
}

