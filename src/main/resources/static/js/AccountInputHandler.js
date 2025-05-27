const AccountInputHandler = {
  bind: function ({
                    accnumSelector = '#ACCNUM',
                    banknmSelector = '#BANKNM',
                    bankidSelector = '#bankid',
                    accidSelector = '#ACCID',
                    minLength = 2
                  }) {
    const $accnum = $(accnumSelector);
    const $banknm = $(banknmSelector);
    const $bankid = $(bankidSelector);
    const $accid = $(accidSelector);

    function setAccountInfo(item) {
      $accnum.val(item.accountNumber);
      $accid.val(item.accid);
      $banknm.val(item.BankName);
      $bankid.val(item.bankId);
    }

    $accnum.on('input', function () {
      if ($(this).val().trim() === '') {
        $banknm.val('');
        $bankid.val('');
        $accid.val('');
      }
    });

    // 엔터 키로 검색
    $accnum.on('keydown', function (e) {
      if (e.key === 'Enter') {
        const accountNumber = $accnum.val().replace(/[\s-]/g, '');
        if (accountNumber.length < minLength) return;

        $.ajax({
          url: '/api/popup/search_Account',
          method: 'GET',
          data: { accountNumber: accountNumber },
          success: function (res) {
            const items = res.data;
            if (!items || items.length === 0) {
              Alert.alert('', '해당 계좌가 존재하지 않습니다.');
            } else if (items.length === 1) {
              setAccountInfo(items[0]);
            } else {
              let poppage = new PopAccountComponent();
              poppage.show(function (item) {
                setAccountInfo(item);
              }, accountNumber);
            }
          },
          error: function () {
            Alert.alert('에러', '계좌 조회 중 오류가 발생했습니다.');
          }
        });
      }
    });
  }
};
