<?php
 
  if(empty($_REQUEST['package']) || empty($_REQUEST['product']) || empty($_REQUEST['tokenid'])){
  echo "error";
  return;
  }
  
  $package = $_REQUEST['package'];
  $product = $_REQUEST['product'];
  $tokenid = $_REQUEST['tokenid'];
  
  $refcode = 'کدی که از بازار گرفتین - رفرش کد';
  
  $url = 'http://pardakht.cafebazaar.ir/auth/token/';
  $data = array('grant_type' => 'refresh_token', 'client_id' => 'آیدی کلاینت', 'client_secret' => 'رمز کلاینت', 'refresh_token' => $refcode);
  
  # Create a connection
  $connection = curl_init($url);
  
  # Form data string
  $postString = http_build_query($data, '', '&');
  
  # Setting our options
  curl_setopt($connection, CURLOPT_POST, 1);
  curl_setopt($connection, CURLOPT_POSTFIELDS, $postString);
  curl_setopt($connection, CURLOPT_RETURNTRANSFER, true);
  
  # Get the response
  $response = curl_exec($connection);
  
  $jsonResponse = json_decode($response, true);
  
  $access_token = $jsonResponse['access_token'];
  
  $result = file_get_contents("https://pardakht.cafebazaar.ir/api/validate/$package/inapp/$product/purchases/$tokenid/?access_token=$access_token");
  
  echo $result;
  
  curl_close($connection);
?>