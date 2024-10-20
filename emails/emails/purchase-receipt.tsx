import {
    Body,
    Button, Column,
    Container,
    Head,
    Html,
    Img,
    Link,
    Preview, Row,
    Section,
    Text,
} from "@react-email/components";
import * as React from "react";

export const PurchaseReceipt = () => (
    <Html>
        <Head>
            <meta name="color-scheme" content="dark"/>
            <meta name="supported-color-schemes" content="dark"/>
        </Head>
        <Preview>Your purchase receipt for %%_RESOURCE_NAME_%%!</Preview>
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
                    <Text style={intro}>Thanks for your purchase!</Text>
                    <Text style={paragraph}>You'll find a copy of your receipt below. <Link style={anchor} href="https://william278.net/account" target="_blank">Sign in</Link> to download your resource and begin using it!</Text>
                    <Row style={receiptContainer}>
                        <Column width="160px">
                            <Img
                                width="140px" height="140px"
                                src="https://thread-assets.william278.net/%%_RESOURCE_NAME_LOWER_%%_icon.png"
                                alt="Square %%_RESOURCE_NAME_%% icon"
                            />
                        </Column>
                        <Column style={receiptDetails}>
                            <Text style={receiptResourceName}><strong>%%_RESOURCE_NAME_%%</strong></Text>
                            <Text style={receiptLine}>Price: %%_RESOURCE_PURCHASE_PRICE_%%</Text>
                            <Text style={receiptLine}>Marketplace: %%_RESOURCE_MARKETPLACE_%%</Text>
                            <Text style={receiptLine}>ID: %%_RESOURCE_TRANSACTION_ID_%%</Text>
                            <Text style={receiptLine}>Time: %%_RESOURCE_TRANSACTION_TIME_%%</Text>
                        </Column>
                    </Row>
                    <Text style={paragraph}>Sign in to William278.net using your Discord account and verify your email address to manage your library and download the version of your purchased resource you need.</Text>
                    <Container style={buttonContainer}>
                        <Button style={button} href="https://william278.net/account">
                            <span style={buttonText}>
                                <span>
                                    <Img src="https://thread-assets.william278.net/email_icon_key_green.png"
                                         width="24px" height="24px"
                                         style={buttonIcon} />
                                </span>
                                <span>Sign in to William278.net</span>
                            </span>
                        </Button>
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

export default PurchaseReceipt;

const main = {
    color: "#f5f5f5",
    backgroundColor: "#222",
    fontFamily: "-apple-system,BlinkMacSystemFont,\"Segue UI\",sans-serif",
    padding: "24px 0"
};

const container = {
    backgroundColor: "#333",
    margin: "0 auto",
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

const buttonContainer = {
    alignItems: "center",
    justifyContent: "center",
    textAlign: "center" as const,
    margin: "20px auto"
};

const receiptContainer = {
    padding: "14px",
    background: "#191919",
    borderRadius: "10px"
};

const receiptDetails = {

}

const receiptResourceName = {
    margin: "0",
    marginBottom: "4px",
    fontSize: "20px",
    lineHeight: "24px"
}

const receiptLine = {
    margin: "0",
    color: "#818181",
    fontFamily: "Consolas,Courier New,monospace",
}

const button = {
    backgroundColor: "none",
    border: "3px solid #00fb9a",
    fontSize: "18px",
    lineHeight: "26px",
    color: "#00fb9a",
    padding: "10px 20px 6px 20px",
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
    paddingTop: "8px",
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

const anchor = {
    color: "#00fb9a",
};

const footerAnchor = {
    fontSize: "16px",
    color: "#00fb9a",
    margin: "0 6px"
};
