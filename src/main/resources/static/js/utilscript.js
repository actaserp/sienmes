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
    const today = new Date();

    const sevenDaysAgo = new Date();
    sevenDaysAgo.setDate(today.getDate() -7);

    const formattedDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };
    return formattedDate(sevenDaysAgo);
}
