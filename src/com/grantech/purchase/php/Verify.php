<?php
 
  if(empty($_REQUEST["market"]) || empty($_REQUEST["product"]) || empty($_REQUEST["token"]))
    die("error");
  $token = $_REQUEST["token"];
  $market = $_REQUEST["market"];
  $product = $_REQUEST["product"];
  
  $packageName = "___";
  $cafebazaarClientId = "___";
  $cafebazaarClientSecret = "___";
  $cafebazaarRefreshToken = "___";
  $zarinpalAccessToken = "___";
  $myketAccessToken = "___";

  // get new access token
  if ($market == "cafebazaar")
  {
    $ch = curl_init("http://pardakht.cafebazaar.ir/auth/token/");
    $data = array("grant_type" => "refresh_token", "client_id" => $cafebazaarClientId, "client_secret" => $cafebazaarClientSecret, "refresh_token" => $cafebazaarRefreshToken);
    $postString = http_build_query($data, '', '&');
    curl_setopt($ch, CURLOPT_POST, 1);
    curl_setopt($ch, CURLOPT_POSTFIELDS, $postString);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    $response = curl_exec($ch);
    $jsonResponse = json_decode($response, true);
    $cafaebazaarAccessToken = $jsonResponse["access_token"];
    curl_close($ch);
  }
  
  $ch = curl_init();
  
  if ($market == "cafebazaar" ) {
    $url = "https://pardakht.cafebazaar.ir/api/validate/$packageName/inapp/$product/purchases/$token/?access_token=$cafaebazaarAccessToken";
  }
  else if ($market == "zarinpal") {
    $url = "https://www.zarinpal.com/pg/rest/WebGate/PaymentVerification.json";
    $data = array("MerchantID" => $zarinpalAccessToken, "Authority" => $token, "Amount" => $product);
    $jsonData = json_encode($data);
    curl_setopt($ch, CURLOPT_USERAGENT, "ZarinPal Rest Api v1");
    curl_setopt($ch, CURLOPT_CUSTOMREQUEST, "POST");
    curl_setopt($ch, CURLOPT_POSTFIELDS, $jsonData);
    curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-Type: application/json", "Content-Length: " . strlen($jsonData)));
  }
  else if($market == "myket") {
    $url = "https://developer.myket.ir/api/applications/$packageName/purchases/products/$product/tokens/$token";
    curl_setopt($ch, CURLOPT_HTTPHEADER, array("Content-Type: application/x-www-form-urlencoded; charset=utf-8", "x-access-token:$myketAccessToken"));
  }
  
  curl_setopt($ch, CURLOPT_URL, $url);
  curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
  $response = curl_exec($ch);
  $err = curl_error($ch);
  $jsonResponse = json_decode($response, true);
  $status = curl_getinfo($ch, CURLINFO_HTTP_CODE);
  curl_close($ch);

  if ($market == "cafebazaar" && $jsonResponse["purchaseState"]."null" == "null")
    $status = 401;
  else if ($market == "zarinpal" && $jsonResponse["Status"] < 100 )
    $status = 401;
  
  $result->status = $status;
  if( $status != 200 )
  {
    echo json_encode($result);
    return;
  }

  if ($market == "zarinpal" )
    $result->consumed = $jsonResponse["Status"] > 100 ? true : false; 
  else if ($market == "cafebazaar" || $market == "ario")
    $result->consumed = $jsonResponse["consumptionState"] == 1; 
  else
    $result->consumed = $jsonResponse["consumptionState"] == 0; 
    
  if ($market != "zarinpal" )
    $result->time = $jsonResponse["purchaseTime"];
      
  echo json_encode($result);
?>