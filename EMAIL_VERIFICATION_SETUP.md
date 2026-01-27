# Email Link Verification Setup Guide

This guide explains how to set up email link verification instead of verification codes for AfyaQuest.

## Overview

Instead of requiring users to manually enter a verification code, users will receive an email with a clickable link that automatically verifies their account.

## Step 1: Deploy the Verification Lambda Function

### 1.1 Install Dependencies

```bash
cd lambda-functions/auth/verify
npm install
```

### 1.2 Create Deployment Package

```bash
zip -r function.zip .
```

### 1.3 Create Lambda Function in AWS

```bash
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

### 1.4 Update Function (if already exists)

```bash
aws lambda update-function-code \
  --function-name auth-verify \
  --zip-file fileb://function.zip \
  --region us-east-1
```

## Step 2: Add API Gateway Endpoint

### 2.1 Create Resource and Method

1. Go to AWS API Gateway Console
2. Select your API (`qsk3ji709f`)
3. Under `/auth` resource, create a new child resource:
   - Resource Name: `verify`
   - Resource Path: `/verify`
4. Create a GET method for `/auth/verify`:
   - Integration Type: Lambda Function
   - Lambda Region: us-east-1
   - Lambda Function: `auth-verify`
   - Use Lambda Proxy Integration: ✅ Yes

### 2.2 Enable CORS

1. Select the `/auth/verify` resource
2. Click "Actions" → "Enable CORS"
3. Keep default settings and confirm

### 2.3 Deploy API

1. Click "Actions" → "Deploy API"
2. Stage: `prod`
3. Confirm deployment

### 2.4 Test the Endpoint

The verification URL will be:
```
https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email=user@example.com&code=123456
```

## Step 3: Configure Cognito Email Template

### Option A: Using AWS Console

1. Go to AWS Cognito Console: https://console.aws.amazon.com/cognito/v2/idp/user-pools?region=us-east-1
2. Select your User Pool: `us-east-1_MxKriIgiC`
3. Go to "Messaging" tab → "Email"
4. Click "Edit" on "Verification message"
5. Select "Email message type": Link
6. Customize the email template:

```html
Hello {username},

Welcome to AfyaQuest! Please verify your email address to complete your registration.

Click the link below to verify your account:

https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email={username}&code={####}

This link will expire in 24 hours.

If you didn't create an account with AfyaQuest, you can safely ignore this email.

Best regards,
The AfyaQuest Team
```

7. Click "Save changes"

### Option B: Using AWS CLI

```bash
aws cognito-idp update-user-pool \
  --user-pool-id us-east-1_MxKriIgiC \
  --email-verification-message "Hello {username},\n\nWelcome to AfyaQuest! Please verify your email by clicking:\n\nhttps://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email={username}&code={####}\n\nThis link expires in 24 hours." \
  --email-verification-subject "Verify your AfyaQuest account" \
  --region us-east-1
```

### Important Notes:

- `{username}` is automatically replaced with the user's email
- `{####}` is automatically replaced with the 6-digit verification code
- The link format must be: `?email={username}&code={####}`

## Step 4: (Optional) Add Deep Linking to Android App

If you want the verification success page to automatically open the app, you need to add deep link support.

### 4.1 Update AndroidManifest.xml

Add this to your main activity in `app/src/main/AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true">

    <!-- Existing intent filters -->
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Deep link for email verification -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="afyaquest"
            android:host="verified" />
    </intent-filter>
</activity>
```

### 4.2 Handle Deep Link in App

In your `MainActivity.kt` or navigation setup, handle the deep link:

```kotlin
// In MainActivity onCreate or in your NavHost
val data: Uri? = intent?.data
if (data?.scheme == "afyaquest" && data.host == "verified") {
    // User clicked verification link
    // Navigate to login or show success message
    navController.navigate("login") {
        popUpTo("splash") { inclusive = true }
    }
}
```

## Step 5: Test the Complete Flow

### 5.1 Register a New User

```bash
curl -X POST https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123#",
    "name": "Test User",
    "phone": "+254712345678"
  }'
```

### 5.2 Check Email

The user should receive an email with a clickable link like:
```
https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/verify?email=test@example.com&code=123456
```

### 5.3 Click Link

When the user clicks the link:
1. They'll see a success page: "✅ Email Verified!"
2. They can now log in to the app
3. (Optional) The "Open AfyaQuest App" button opens the app via deep link

### 5.4 Verify User Can Login

```bash
curl -X POST https://qsk3ji709f.execute-api.us-east-1.amazonaws.com/prod/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "TestPass123#"
  }'
```

Should now return tokens successfully (no "UserNotConfirmedException" error).

## Alternative: Resend Verification Code

If you want to add a "Resend verification email" feature, create another Lambda function:

### Create `auth-resend-verification` Lambda

```javascript
const { CognitoIdentityProviderClient, ResendConfirmationCodeCommand } = require("@aws-sdk/client-cognito-identity-provider");

const cognitoClient = new CognitoIdentityProviderClient({ region: 'us-east-1' });

exports.handler = async (event) => {
    const body = JSON.parse(event.body);
    const { email } = body;

    const command = new ResendConfirmationCodeCommand({
        ClientId: process.env.COGNITO_CLIENT_ID,
        Username: email
    });

    await cognitoClient.send(command);

    return {
        statusCode: 200,
        body: JSON.stringify({
            message: "Verification email resent successfully"
        })
    };
};
```

## Troubleshooting

### Issue: Link says "Invalid verification code"

**Cause**: The code in the URL might be incorrect or expired.

**Solution**:
- Verification codes expire after 24 hours
- User needs to request a new verification email
- Or manually confirm user:
  ```bash
  aws cognito-idp admin-confirm-sign-up \
    --user-pool-id us-east-1_MxKriIgiC \
    --username user@example.com \
    --region us-east-1
  ```

### Issue: Email not received

**Cause**: Cognito email limits or email in spam folder.

**Solution**:
- Check spam/junk folder
- Verify Cognito email sending limits (default is 50 emails/day in sandbox mode)
- For production, configure SES (Simple Email Service) for higher limits

### Issue: Link doesn't open app

**Cause**: Deep linking not configured or app not installed.

**Solution**:
- Ensure AndroidManifest.xml has the deep link intent filter
- User must have the app installed for deep links to work
- Test deep link: `adb shell am start -a android.intent.action.VIEW -d "afyaquest://verified"`

## Benefits of Link Verification

✅ **Better UX**: Users just click a link instead of copying/pasting codes
✅ **Mobile Friendly**: Works seamlessly on mobile devices
✅ **Fewer Errors**: No typos when entering codes manually
✅ **Professional**: Matches industry standards (similar to most apps)
✅ **Deep Linking**: Can automatically open the app after verification

## Next Steps

1. Deploy the Lambda function (Step 1)
2. Add the API Gateway endpoint (Step 2)
3. Update Cognito email template (Step 3)
4. Test with a new user registration
5. (Optional) Add deep linking support in Android app (Step 4)

---

**Status**: Ready to deploy
**Last Updated**: 2026-01-27
**Tested**: Pending deployment
