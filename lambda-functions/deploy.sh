#!/bin/bash

# AfyaQuest Lambda Deployment Script
# This script packages and deploys all Lambda functions

set -e

echo "🚀 Starting Lambda deployment..."

# Configuration
REGION="af-south-1"
ROLE_ARN="arn:aws:iam::756401225362:role/AfyaQuest-Lambda-Role"
RUNTIME="nodejs20.x"
TIMEOUT=30
MEMORY=512

# Environment variables
TABLE_NAME="AfyaQuestData"
COGNITO_CLIENT_ID="${COGNITO_CLIENT_ID:-YOUR_CLIENT_ID}"
CLOUDFRONT_DOMAIN="${CLOUDFRONT_DOMAIN:-d2c7svayh8fll3.cloudfront.net}"
S3_BUCKET="afyaquest-media-756401225362"
BEDROCK_MODEL_ID="anthropic.claude-3-5-sonnet-20241022-v2:0"

# Array of Lambda functions
declare -A LAMBDAS=(
    ["auth/register"]="auth-register"
    ["auth/login"]="auth-login"
    ["auth/getCurrentUser"]="auth-getCurrentUser"
    ["auth/verify"]="auth-verify"
    ["chat/sendMessage"]="chat-sendMessage"
    ["questions/getDaily"]="questions-getDaily"
    ["progress/getUser"]="progress-getUser"
    ["progress/updateLesson"]="progress-updateLesson"
    ["progress/submitQuiz"]="progress-submitQuiz"
    ["progress/updateStreak"]="progress-updateStreak"
)

# Function to deploy a Lambda
deploy_lambda() {
    local path=$1
    local name=$2

    echo "📦 Deploying $name..."

    cd "$path"

    # Install dependencies if package.json exists
    if [ -f "package.json" ]; then
        echo "  📥 Installing dependencies..."
        npm install --production --silent
    fi

    # Create deployment package
    echo "  📦 Creating deployment package..."
    zip -q -r function.zip . -x "*.git*" "*.DS_Store"

    # Check if function exists
    if aws lambda get-function --function-name "$name" --region "$REGION" &> /dev/null; then
        echo "  🔄 Updating existing function..."
        aws lambda update-function-code \
            --function-name "$name" \
            --zip-file fileb://function.zip \
            --region "$REGION" \
            --no-cli-pager > /dev/null

        # Update configuration
        aws lambda update-function-configuration \
            --function-name "$name" \
            --timeout "$TIMEOUT" \
            --memory-size "$MEMORY" \
            --environment "Variables={
                DYNAMODB_TABLE=$TABLE_NAME,
                COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID,
                S3_BUCKET=$S3_BUCKET,
                CLOUDFRONT_DOMAIN=$CLOUDFRONT_DOMAIN,
                BEDROCK_MODEL_ID=$BEDROCK_MODEL_ID,
                AWS_REGION=$REGION,
                NODE_ENV=production
            }" \
            --region "$REGION" \
            --no-cli-pager > /dev/null
    else
        echo "  ✨ Creating new function..."
        aws lambda create-function \
            --function-name "$name" \
            --runtime "$RUNTIME" \
            --role "$ROLE_ARN" \
            --handler index.handler \
            --zip-file fileb://function.zip \
            --timeout "$TIMEOUT" \
            --memory-size "$MEMORY" \
            --environment "Variables={
                DYNAMODB_TABLE=$TABLE_NAME,
                COGNITO_CLIENT_ID=$COGNITO_CLIENT_ID,
                S3_BUCKET=$S3_BUCKET,
                CLOUDFRONT_DOMAIN=$CLOUDFRONT_DOMAIN,
                BEDROCK_MODEL_ID=$BEDROCK_MODEL_ID,
                AWS_REGION=$REGION,
                NODE_ENV=production
            }" \
            --region "$REGION" \
            --no-cli-pager > /dev/null
    fi

    # Clean up
    rm function.zip

    echo "  ✅ $name deployed successfully"

    cd - > /dev/null
}

# Main deployment
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

for path in "${!LAMBDAS[@]}"; do
    name="${LAMBDAS[$path]}"
    full_path="$SCRIPT_DIR/$path"

    if [ -d "$full_path" ]; then
        deploy_lambda "$full_path" "$name"
    else
        echo "⚠️  Warning: Directory not found: $full_path"
    fi
done

echo ""
echo "✅ All Lambda functions deployed successfully!"
echo ""
echo "📝 Next steps:"
echo "  1. Configure Cognito User Pool ID in local.properties"
echo "  2. Set up API Gateway endpoints"
echo "  3. Create Cognito authorizer"
echo "  4. Test endpoints with Postman/curl"
