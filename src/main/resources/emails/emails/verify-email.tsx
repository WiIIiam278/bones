import {
    Body,
    Button,
    Container,
    Head,
    Html,
    Img,
    Link,
    Preview,
    Section,
    Text,
} from "@react-email/components";
import * as React from "react";

export const VerifyEmail = () => (
    <Html>
        <Head>
            <meta name="color-scheme" content="dark"/>
            <meta name="supported-color-schemes" content="dark"/>
        </Head>
        <Preview>Verify your William278.net email, @%%_USERNAME_%%.</Preview>
        <Body style={main}>
            <Container style={container}>
                <Container style={title}>
                    <Img
                        style={titleIcon}
                        width="90px" height="90px"
                        src="https://thread-assets.william278.net/william278_icon_transparent_small.png"
                    />
                </Container>
                <Container style={body}>
                    <Text style={intro}>Verify your email, <strong>@%%_USERNAME_%%</strong></Text>
                    <Text style={paragraph}>
                        Nearly there! Click the link below to verify your email. Purchases made using this email will
                        then be automatically imported to your profile.
                    </Text>
                    <Container style={verification}>
                        <Button style={button} href="%%_VERIFY_URL_%%">
                            <span style={buttonText}>
                                <span>
                                    <Img src="https://thread-assets.william278.net/email_icon_envelope_green.png"
                                           width="24px" height="24px"
                                           style={buttonIcon} />
                                </span>
                                <span>Verify your email</span>
                            </span>
                        </Button>
                        <Text style={verifyDivider}>
                            or enter code:
                        </Text>
                        <Text style={verifyCode}>
                            %%_VERIFY_CODE_%%
                        </Text>
                    </Container>
                    <Text style={paragraph}>If you need help with managing your account, don't hesitate to reach out on Discord for support.</Text>
                    <Text style={paragraph}>— William278.net</Text>
                </Container>
            </Container>
            <Container style={footer}>
                <Text style={footerCopyright}>&copy; William278, 2024</Text>
                <Section style={footerLinks}>
                    <Link href="https://william278.net/terms#terms-and-conditions" style={footerAnchor}>Terms</Link>&ndash;
                    <Link href="https://william278.net/terms#privacy-notice" style={footerAnchor}>Privacy</Link>&ndash;
                    <Link href="https://status.william278.net/" style={footerAnchor}>Status</Link>&ndash;
                    <Link href="https://william278.net/account" style={footerAnchor}>Account</Link>
                </Section>
            </Container>
        </Body>
    </Html>
);

export default VerifyEmail;

const main = {
    color: "#f5f5f5",
    backgroundColor: "#222",
    fontFamily: "-apple-system,BlinkMacSystemFont,\"Segue UI\",sans-serif",
    padding: "0 32px 0"
};

const container = {
    backgroundColor: "#333",
    margin: "16px auto",
    padding: "0",
    borderRadius: "10px",
    boxShadow: "0 0 0.75rem rgba(0, 0, 0, 0.1)"
};

const body = {
    padding: "15px 30px"
};

const title = {
    justifyContent: "center",
    alignItems: "center",
    width: "100%",
    height: "130px",
    background: "linear-gradient(180deg, rgba(18,39,60,1) 0%, rgba(8,17,27,1) 100%)",
    borderRadius: "10px 10px 0 0",
}

const titleIcon = {
    textAlign: "center" as const,
    margin: "0 auto",
    padding: "0 auto",
    width: "90px",
    height: "90px",
}

const intro = {
    fontSize: "24px",
    lineHeight: "36px",
    fontWeight: "bold",
    textAlign: "left" as const,
}

const paragraph = {
    fontSize: "16px",
    lineHeight: "24px",
    textAlign: "left" as const,
};

const verification = {
    alignItems: "center",
    justifyContent: "center",
    textAlign: "center" as const,
    margin: "40px auto"
};

const verifyCode = {
    color: "#818181",
    fontFamily: "Consolas,Courier New,monospace",
    fontSize: "26px",
    lineHeight: "20px",
    fontWeight: "bold",
    backgroundColor: "#191919",
    padding: "20px",
    margin: "0 auto",
    borderRadius: "10px",
    maxWidth: "250px",
};

const verifyDivider = {
    color: "#818181",
};

const button = {
    backgroundColor: "#00fb9a11",
    boxShadow: "#00fb9a33 0 0 70px",
    border: "3px solid #00fb9a",
    fontSize: "18px",
    lineHeight: "26px",
    color: "#00fb9a",
    padding: "12px 20px 8px 20px",
    borderRadius: "50px",
    fontWeight: "bold",
    textAlign: "center" as const
};

const buttonText = {
    display: "flex",
    justifyContent: "center",
    alignItems: "center"
}

const buttonIcon = {
    paddingRight: "10px",
    fontSize: "24px",
    lineHeight: "26px"
}

const footer = {
    justifyContent: "center",
    alignItems: "center",
    textAlign: "center" as const,
    color: "#818181",
    margin: "0 auto"
};

const footerLinks = {
    margin: "0 auto",
    padding: "0 auto",
    textAlign: "center" as const,
    justifyContent: "center",
    alignItems: "center"
}

const footerCopyright = {
    fontSize: "16px",
    margin: "2px 0"
}

const footerAnchor = {
    color: "#00fb9a",
    margin: "0 6px"
};
