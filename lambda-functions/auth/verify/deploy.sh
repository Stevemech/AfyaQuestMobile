#!/bin/bash

# Deploy auth-verify Lambda function
# Usage: ./deploy.sh

set -e

echo "🚀 Deploying auth-verify Lambda function..."

# Install dependencies
echo "📦 Installing dependencies..."
npm install

# Create deployment package
echo "📦 Creating deployment package..."
zip -r function.zip . -x "*.sh" -x "*.md"

# Check if function exists
if aws lambda get-function --function-name auth-verify --region af-south-1 >/dev/null 2>&1; then
    echo "♻️  Updating existing function..."
    aws lambda update-function-code \
        --function-name auth-verify \
        --zip-file fileb://function.zip \
        --region af-south-1
else
    echo "🆕 Creating new function..."
    aws lambda create-function \
        --function-name auth-verify \
        --runtime nodejs20.x \
        --role arn:aws:iam::756401225362:role/AfyaQuest-Lambda-Role \
        --handler index.handler \
        --zip-file fileb://function.zip \
        --timeout 30 \
        --memory-size 512 \
        --region af-south-1 \
        --environment Variables="{COGNITO_CLIENT_ID=70vjg3trmh44me8b7bnvm6e8va}"
fi

echo "✅ Lambda function deployed successfully!"
echo ""
echo "Next steps:"
echo "1. Add GET /auth/verify endpoint in API Gateway"
echo "2. Update Cognito email template to use verification links"
echo "3. Test with a new user registration"
echo ""
echo "📚 See EMAIL_VERIFICATION_SETUP.md for detailed instructions"
