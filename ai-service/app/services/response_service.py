class ResponseService:

    RESPONSES = {
        "AUTHENTICATION": (
            "Please verify your credentials and try resetting your password. "
            "If the issue persists, contact our support team."
        ),
        "PAYMENT": (
            "Please verify your payment information and try again. "
            "If the payment was declined, check with your bank."
        ),
        "TECHNICAL": (
            "Our technical team will investigate this issue. "
            "Please provide more details about the error."
        ),
        "BUG": (
            "Thank you for reporting this issue. "
            "Our technical team will investigate and fix it."
        ),
        "SUBSCRIPTION": (
            "Please provide more information about your subscription. "
            "We will help you resolve this issue."
        ),
        "ACCOUNT": (
            "Please verify your account information and provide more details. "
            "We will help you update your account."
        ),
        "DEFAULT": (
            "Thank you for contacting support. "
            "Our team will investigate your request."
        )
    }

    def generate_response(self, category: str) -> str:
        return self.RESPONSES.get(
            category,
            self.RESPONSES["DEFAULT"]
        )