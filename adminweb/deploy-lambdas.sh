#!/bin/bash
set -e

PROFILE="afyaquest"
REGION="af-south-1"
ROLE_ARN="arn:aws:iam::756401225362:role/AfyaQuest-Lambda-Role"
API_ID="gc6iib7ck2"
LAMBDA_DIR="../lambda-functions/admin"
AUTHORIZER_ID="ezwjbs"

echo "=== Deploying AfyaQuest Admin Lambda Functions ==="

deploy_lambda() {
  local FUNC_NAME=$1
  local FUNC_DIR=$2
  local HANDLER=${3:-index.handler}

  echo ""
  echo "--- Deploying $FUNC_NAME ---"

  # Create zip
  cd "$FUNC_DIR"
  zip -r /tmp/${FUNC_NAME}.zip . > /dev/null
  cd - > /dev/null

  # Check if function exists
  if aws lambda get-function --function-name "$FUNC_NAME" --profile "$PROFILE" --region "$REGION" > /dev/null 2>&1; then
    echo "  Updating existing function..."
    aws lambda update-function-code \
      --function-name "$FUNC_NAME" \
      --zip-file "fileb:///tmp/${FUNC_NAME}.zip" \
      --profile "$PROFILE" \
      --region "$REGION" \
      --output text --query 'FunctionArn'
  else
    echo "  Creating new function..."
    aws lambda create-function \
      --function-name "$FUNC_NAME" \
      --runtime nodejs20.x \
      --role "$ROLE_ARN" \
      --handler "$HANDLER" \
      --zip-file "fileb:///tmp/${FUNC_NAME}.zip" \
      --timeout 15 \
      --memory-size 256 \
      --profile "$PROFILE" \
      --region "$REGION" \
      --output text --query 'FunctionArn'
  fi

  echo "  Done."
}

add_api_route() {
  local METHOD=$1
  local ROUTE_PATH=$2
  local FUNC_NAME=$3
  local USE_AUTH=${4:-true}

  echo "  Adding route: $METHOD $ROUTE_PATH -> $FUNC_NAME"

  # Get Lambda ARN
  LAMBDA_ARN=$(aws lambda get-function --function-name "$FUNC_NAME" --profile "$PROFILE" --region "$REGION" --output text --query 'Configuration.FunctionArn')

  # Create integration
  INTEGRATION_ID=$(aws apigatewayv2 create-integration \
    --api-id "$API_ID" \
    --integration-type AWS_PROXY \
    --integration-uri "$LAMBDA_ARN" \
    --payload-format-version "2.0" \
    --profile "$PROFILE" \
    --region "$REGION" \
    --output text --query 'IntegrationId' 2>/dev/null || echo "")

  if [ -z "$INTEGRATION_ID" ]; then
    echo "  Warning: Could not create integration for $FUNC_NAME"
    return
  fi

  # Create route
  if [ "$USE_AUTH" = "true" ]; then
    aws apigatewayv2 create-route \
      --api-id "$API_ID" \
      --route-key "$METHOD $ROUTE_PATH" \
      --target "integrations/$INTEGRATION_ID" \
      --authorization-type JWT \
      --authorizer-id "$AUTHORIZER_ID" \
      --profile "$PROFILE" \
      --region "$REGION" \
      --output text --query 'RouteId' 2>/dev/null || echo "  Route may already exist"
  else
    aws apigatewayv2 create-route \
      --api-id "$API_ID" \
      --route-key "$METHOD $ROUTE_PATH" \
      --target "integrations/$INTEGRATION_ID" \
      --profile "$PROFILE" \
      --region "$REGION" \
      --output text --query 'RouteId' 2>/dev/null || echo "  Route may already exist"
  fi

  # Add Lambda permission for API Gateway
  ACCOUNT_ID="756401225362"
  STATEMENT_ID="apigateway-${FUNC_NAME}-$(date +%s)"
  aws lambda add-permission \
    --function-name "$FUNC_NAME" \
    --statement-id "$STATEMENT_ID" \
    --action lambda:InvokeFunction \
    --principal apigateway.amazonaws.com \
    --source-arn "arn:aws:execute-api:${REGION}:${ACCOUNT_ID}:${API_ID}/*" \
    --profile "$PROFILE" \
    --region "$REGION" > /dev/null 2>&1 || true

  echo "  Route added."
}

# Deploy Lambda functions
deploy_lambda "admin-getCHVs" "$LAMBDA_DIR/getCHVs"
deploy_lambda "admin-getCHVDetail" "$LAMBDA_DIR/getCHVDetail"
deploy_lambda "admin-getAnalytics" "$LAMBDA_DIR/getAnalytics"
deploy_lambda "admin-getReports" "$LAMBDA_DIR/getReports"
deploy_lambda "admin-assignTask" "$LAMBDA_DIR/assignTask"
deploy_lambda "admin-getClinics" "$LAMBDA_DIR/getClinics"
deploy_lambda "admin-createItinerary" "$LAMBDA_DIR/createItinerary"

echo ""
echo "=== Setting up API Gateway routes ==="

add_api_route "GET" "/admin/chvs" "admin-getCHVs"
add_api_route "GET" "/admin/chvs/{chvId}" "admin-getCHVDetail"
add_api_route "GET" "/admin/analytics" "admin-getAnalytics"
add_api_route "GET" "/admin/reports" "admin-getReports"
add_api_route "POST" "/admin/assign" "admin-assignTask"
add_api_route "GET" "/admin/clinics" "admin-getClinics"
add_api_route "POST" "/admin/itineraries" "admin-createItinerary"

echo ""
echo "=== Deploying API ==="
aws apigatewayv2 create-deployment \
  --api-id "$API_ID" \
  --profile "$PROFILE" \
  --region "$REGION" \
  --output text --query 'DeploymentId'

echo ""
echo "=== Deployment complete! ==="
echo "API URL: https://${API_ID}.execute-api.${REGION}.amazonaws.com/prod/"
echo ""
echo "Admin routes:"
echo "  GET  /admin/chvs"
echo "  GET  /admin/chvs/{chvId}"
echo "  GET  /admin/analytics"
echo "  GET  /admin/reports"
echo "  POST /admin/assign"
echo "  GET  /admin/clinics"
echo "  POST /admin/itineraries"
