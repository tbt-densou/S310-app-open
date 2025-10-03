function doPost(e) {
  try {
    var sheet = SpreadsheetApp.openById("1KhUaEoXtNmOfLaggssr1liDojRXz5BrmrxD_2d4AasM").getSheetByName("Sheet1");
    var data = JSON.parse(e.postData.contents);
    
    // カンマ区切りで値を分割し、配列に格納
    var values = data.value.split(",");
    
    // 日本時間のタイムスタンプを追加
    var now = new Date();
    var timestamp = new Date(now.getTime() + (9 * 60 * 60 * 1000)); // 日本時間に調整
    var formattedTimestamp = timestamp.toISOString().replace('T', ' ').replace('Z', '');
    formattedTimestamp = formattedTimestamp + '.' + timestamp.getMilliseconds().toString().padStart(3, '0');
    values.unshift(formattedTimestamp); // valuesの最初に日時を追加

    Logger.log(values);

    // スプレッドシートにデータを追加（`setValues()`を使用）
    var lastRow = sheet.getLastRow() + 1;
    sheet.getRange(lastRow, 1, 1, values.length).setValues([values]);

    return ContentService.createTextOutput("Success");
  } catch (error) {
    Logger.log("Error in doPost: " + error);
    return ContentService.createTextOutput("Error: " + error.message);
  }
}





function doGet(e) {
    var sheet = SpreadsheetApp.openById("1KhUaEoXtNmOfLaggssr1liDojRXz5BrmrxD_2d4AasM").getSheetByName("Sheet1");
    var data = sheet.getDataRange().getValues();
    
    return ContentService.createTextOutput(JSON.stringify(data)).setMimeType(ContentService.MimeType.JSON);
}