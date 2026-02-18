/**
 * Lambda Function: auth-customMessage
 * Cognito Custom Message trigger - sends verification emails with clickable links
 */

const VERIFY_URL = process.env.VERIFY_URL || 'https://gc6iib7ck2.execute-api.af-south-1.amazonaws.com/prod/auth/verify';

exports.handler = async (event) => {
    console.log('CustomMessage trigger:', JSON.stringify(event, null, 2));

    if (event.triggerSource === 'CustomMessage_SignUp' || event.triggerSource === 'CustomMessage_ResendCode') {
        const email = event.request.userAttributes.email;
        const code = event.request.codeParameter;
        const name = event.request.userAttributes.name || 'there';
        const verifyLink = `${VERIFY_URL}?email=${encodeURIComponent(email)}&code=${code}`;

        event.response.emailSubject = 'Verify your AfyaQuest account';
        event.response.emailMessage = `
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
</head>
<body style="margin:0;padding:0;background:#f5f5f5;font-family:Arial,sans-serif;">
    <table width="100%" cellpadding="0" cellspacing="0" style="background:#f5f5f5;padding:40px 0;">
        <tr>
            <td align="center">
                <table width="500" cellpadding="0" cellspacing="0" style="background:white;border-radius:10px;box-shadow:0 2px 10px rgba(0,0,0,0.1);padding:40px;">
                    <tr>
                        <td align="center" style="padding-bottom:20px;">
                            <h1 style="color:#4caf50;margin:0;font-size:28px;">AfyaQuest</h1>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding-bottom:20px;color:#333;font-size:16px;line-height:1.5;">
                            <p>Hi ${name},</p>
                            <p>Welcome to AfyaQuest! Please verify your email address to get started.</p>
                        </td>
                    </tr>
                    <tr>
                        <td align="center" style="padding-bottom:25px;">
                            <a href="${verifyLink}"
                               style="display:inline-block;background:#4caf50;color:white;padding:14px 40px;border-radius:5px;text-decoration:none;font-weight:bold;font-size:16px;">
                                Verify My Email
                            </a>
                        </td>
                    </tr>
                    <tr>
                        <td style="padding-bottom:20px;color:#666;font-size:14px;text-align:center;">
                            <p>Or enter this code manually in the app: <strong>${code}</strong></p>
                        </td>
                    </tr>
                    <tr>
                        <td style="border-top:1px solid #eee;padding-top:20px;color:#999;font-size:12px;text-align:center;">
                            <p>If you didn't create an AfyaQuest account, you can safely ignore this email.</p>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
    </table>
</body>
</html>`;
    }

    return event;
};
