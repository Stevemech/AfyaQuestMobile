# Quick Start: Email Link Verification

## What Changed

Instead of users entering a verification code manually, they now receive an email with a clickable link that automatically verifies their account.

## Changes Made

### ✅ Backend (Lambda Function)
- Created `lambda-functions/auth/verify/` - handles email verification via link
- Returns a nice HTML success page when verification is complete

### ✅ Android App
- Updated `AndroidManifest.xml` - added deep link support for `afyaquest://verified`
- Updated `MainActivity.kt` - handles deep link and shows "Email verified!" toast message

### ⏳ AWS Configuration (You need to do this)
Follow these 3 steps to deploy:

## Step 1: Deploy Lambda Function (5 minutes)

```bash
cd lambda-functions/auth/verify
./deploy.sh
```

Or manually:
```bash
cd lambda-functions/auth/verify
npm install
zip -r function.zip .
aws lambda create-function \
  --function-name auth-verify \
  --runtime nodejs20.x \
  --role arn:aws:iam::556683673972:role/AfyaQuest-Lambda-Role \
  --handler index.handler \
  --zip-file fileb://function.zip \
  --timeout 30 \
  --memory-size 512 \
  --region us-east-1 \
  --environment Variables="{COGNITO_CLIENT_ID=3qkth9j9bcvvhl9b6q7ml7qih}"
```

## Step 2: Add API Gateway Endpoint (3 minutes)

### Option A: AWS Console (Easier)
1. Go to https://console.aws.amazon.com/apigateway
2. Open API: `qsk3ji709f`
3. Under `/auth`, create resource: `verify`
4. Add method: `GET`
5. Integration type: Lambda Function
6. Function: `auth-verify`
7. Enable Lambda Proxy Integration
8. Enable CORS
9. Deploy to `prod` stage

### Option B: AWS CLI
```bash
# Get the /auth resource ID
AUTH_RESOURCE_ID=$(aws apigateway get-resources \
  --rest-api-id qsk3ji709f \
  --region us-east-1 \
  --query 'items[?path==`/auth`].id' \
  --output text)

# Create /auth/verify resource
VERIFY_RESOURCE_ID=$(aws apigateway create-resource \
  --rest-api-id qsk3ji709f \
  --parent-id $AUTH_RESOURCE_ID \
  --path-part verify \
  --region us-east-1 \
  --query 'id' \
  --output text)

# Add GET method
aws apigateway put-method \
  --rest-api-id qsk3ji709f \
  --resource-id $VERIFY_RESOURCE_ID \
  --http-method GET \
  --authorization-type NONE \
  --region us-east-1

# Integrate with Lambda
LAMBDA_ARN="arn:aws:lambda:us-east-1:556683673972:function:auth-verify"
aws apigateway put-integration \
  --rest-api-id qsk3ji709f \
  --resource-id $VERIFY_RESOURCE_ID \
  --http-method GET \
  --type AWS_PROXY \
  --integration-http-method POST \
  --uri "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/${LAMBDA_ARN}/invocations" \
  --region us-east-1

# Grant API Gateway permission to invoke Lambda
aws lambda add-permission \
  --function-name auth-verify \
  --statement-id apigateway-verify-get \
  --action lambda:InvokeFunction \
  --principal apigateway.amazonaws.com \
  --source-arn "arn:aws:execute-api:us-east-1:556683673972:qsk3ji709f/*/GET/auth/verify" \
  --region us-east-1

# Enable CORS
aws apigateway put-method \
  --rest-api-id qsk3ji709f \
  --resource-id $VERIFY_RESOURCE_ID \
  --http-method OPTIONS \
  --authorization-type NONE \
  --region us-east-1

# Deploy API
aws apigateway create-deployment \
  --rest-api-id qsk3ji709f \
  --stage-name prod \
  --region us-east-1
```

## Step 3: Update Cognito Email Template (2 minutes)

### Option A: AWS Console
1. Go to https://console.aws.amazon.com/cognito/v2/idp/user-pools?region=us-east-1
2. Select User Pool: `us-east-1_MxKriIgiC`
3. Go to "Messaging" → "Email"
4. Click "Edit"
5. Update verification message:

**Subject:**
```
Verify your AfyaQuest account
```

**Message (replace the current message):**
```
Hello {username},

Welcome to AfyaQuest! Please verify your email address to complete your registration.

Click the link below to verify your account:

https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email={username}&code={####}

This link will expire in 24 hours.

If you didn't create an account with AfyaQuest, you can safely ignore this email.

Best regards,
The AfyaQuest Team
```

6. Save changes

### Option B: AWS CLI
```bash
aws cognito-idp update-user-pool \
  --user-pool-id us-east-1_MxKriIgiC \
  --email-verification-message "Hello {username},\n\nWelcome to AfyaQuest! Please verify your email by clicking:\n\nhttps://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email={username}&code={####}\n\nThis link expires in 24 hours." \
  --email-verification-subject "Verify your AfyaQuest account" \
  --region us-east-1
```

## Test It

### 1. Register a new user in the app
- Open the Android app
- Click "Sign Up"
- Fill in details and register

### 2. Check email
- You'll receive an email with subject "Verify your AfyaQuest account"
- Click the verification link

### 3. Verification page opens
- You'll see "✅ Email Verified!" page
- Click "Open AfyaQuest App" (deep link)
- Or manually open the app

### 4. Login
- The app shows "Email verified!" toast
- Go to login screen
- Enter credentials
- You should be able to login successfully!

## Troubleshooting

### "Email not received"
- Check spam/junk folder
- Cognito has email limits (50/day in sandbox mode)
- Check CloudWatch logs: `/aws/lambda/auth-register`

### "Invalid verification code"
- Code expired (24 hours validity)
- Manually confirm user:
  ```bash
  aws cognito-idp admin-confirm-sign-up \
    --user-pool-id us-east-1_MxKriIgiC \
    --username user@example.com \
    --region us-east-1
  ```

### "Deep link doesn't open app"
- Make sure app is installed
- Rebuild app after AndroidManifest changes
- Test deep link manually:
  ```bash
  adb shell am start -a android.intent.action.VIEW -d "afyaquest://verified"
  ```

## For Existing Users

If you have existing unverified users, manually confirm them:

```bash
# List all users
aws cognito-idp list-users \
  --user-pool-id us-east-1_MxKriIgiC \
  --region us-east-1

# Confirm specific user
aws cognito-idp admin-confirm-sign-up \
  --user-pool-id us-east-1_MxKriIgiC \
  --username user@example.com \
  --region us-east-1
```

## Benefits

✅ Better user experience - just click a link
✅ Mobile-friendly - works seamlessly on phones
✅ Fewer errors - no manual code entry
✅ Professional - industry standard approach
✅ Auto-opens app - deep linking after verification

---

**Need help?** See `EMAIL_VERIFICATION_SETUP.md` for detailed instructions.
