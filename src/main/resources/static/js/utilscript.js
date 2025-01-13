function submitTextarea(event, func){
    let key = event.key || event.keyCode;

    if(key === 'Enter' || key == 13){
        func();
    }
}