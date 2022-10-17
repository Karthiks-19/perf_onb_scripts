import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.util.Random
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory


class OnboardingApiSimulationsAzpire extends Simulation {

   val conf = ConfigFactory.load();
   val url = conf.getString("HOST");
  val httpProtocol = http
     .baseUrl(url)
//    .baseUrl("https://stg-api.zolve.com")
    .acceptHeader(" application/json")
    .acceptEncodingHeader("gzip, deflate")

  val userCount = Integer.getInteger("Users", 1).toInt
  val testDuration = Integer.getInteger("Duration", 10).toInt

  before {
    println(s"Running test with ${userCount} users")
    println(s"User email is ${randomEmailGenerator} users")
     println(s"Running test with ${testDuration} seconds")
     println(s"Running test with host ${url}")
  }

  val randomEmailGenerator = s"${Random.alphanumeric.take(5).mkString}@test.com"
  val randomNumberGenerator = (Random.nextInt(900000000)+100000000).toString

  val scn = scenario("OnboardingSimulation with v3")
    .exec(_.set("random_email", randomEmailGenerator))
    .exec(http("create new user")
      .post("/api/v1/account/registration/create-temp-user/")
      .header("content-type", "application/json")
      .body(ElFileBody("onboarding_v3/common/create_user.json"))
      .check(jsonPath("$..temp_user_id").find.saveAs("temp_user_id"))
    )

    .exec(http("Enter email OTP")
      .post("/api/v1/account/registration/verify-otp/")
      .header("content-type", "application/json")
      .body(ElFileBody("onboarding_v3/common/email_OTP.json"))
      .check(jsonPath("$.code").is("200"))
    )

    .exec(_.set("random_phone_number", randomNumberGenerator))
    .exec(http("Enter mobile number")
      .post("/api/v1/account/registration/add-identity/")
      .header("content-type", "application/json")
      .body(ElFileBody("onboarding_v3/common/mobile_number.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$..status").is("SUCCESS"))
    )

    .exec(http("Enter mobile number OTP")
      .post("/api/v1/account/registration/verify-otp/")
      .header("content-type", "application/json")
      .body(ElFileBody("onboarding_v3/common/mobile_number_OTP.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$..status").is("SUCCESS"))
      .check(jsonPath("$..access_token").find.saveAs("access_token"))
    )

    .exec(http("Profile update")
      .post("/api/v1/account/profile-update/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(RawFileBody("onboarding_v3/common/profile_update.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Profile updated."))
    )

    .exec(http("Create bank account and crebit account")
      .post("/api/v3/application/create/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(RawFileBody("onboarding_v3/azpire/create_bank_and_crebit.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Applications Created"))
      .check(jsonPath("$..applicationId").find(0).saveAs("application_Id"))
      .check(jsonPath("$..applicationId").find(1).saveAs("crebit_applicationId"))
    )
    
     .exec(http("Next api for task")
       .post("/api/v1/application/task/next/")
       .header("content-type", "application/json")
       .header("Authorization", "Bearer ${access_token}")
       .body(ElFileBody("onboarding_v3/common/next.json"))
       .check(jsonPath("$.code").is("200"))
       .check(jsonPath("$.message").is("Fetched next task."))
       .check(jsonPath("$..taskId").find(0).saveAs("taskId"))
     )

    .exec(http("Upload passport")
      .post("/api/v1/application/task/process/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/common/upload_passport.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Processing task."))
    )

    .exec(http("Next api for task")
      .post("/api/v1/application/task/next/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/common/next.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Fetched next task."))
      .check(jsonPath("$..taskId").find.saveAs("taskId_01"))
    )

    .exec(http("Skip address")
      .post("/api/v1/application/task/process/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/common/skip_address.json"))
      .check(jsonPath("$.code").is("200"))
    )

    .exec(http("Next api for task")
      .post("/api/v1/application/task/next/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/common/next.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Fetched next task."))
      .check(jsonPath("$..taskId").find.saveAs("taskId_02"))
    )

    .exec(http("Confirm bank T&C")
      .post("/api/v1/application/task/process/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/common/confirm_bank.json"))
      .check(jsonPath("$.code").is("200"))
    )

    .exec(http("Next api for task")
      .post("/api/v1/application/task/next/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/azpire/next_api_crebit.json"))
      .check(jsonPath("$.code").is("200"))
      .check(jsonPath("$.message").is("Fetched next task."))
      .check(jsonPath("$..taskId").find.saveAs("taskId_03"))
    )

    .exec(http("Confirm crebit T&C")
      .post("/api/v1/application/task/process/")
      .header("content-type", "application/json")
      .header("Authorization", "Bearer ${access_token}")
      .body(ElFileBody("onboarding_v3/azpire/confirm_crebit.json"))
      .check(jsonPath("$.code").is("200"))
    )


  //   setUp(scn.inject(constantConcurrentUsers(userCount).during(testDuration))).protocols(httpProtocol)
  setUp(scn.inject(atOnceUsers(userCount))).protocols(httpProtocol)
}
