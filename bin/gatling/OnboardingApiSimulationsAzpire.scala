import io.gatling.core.Predef._
import io.gatling.http.Predef._

import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import io.gatling.core.feeder.Random

import java.nio.charset.StandardCharsets
import scala.util.Random


class OnboardingApiSimulationsAzpire extends Simulation {

//   val conf = ConfigFactory.load();
//   val url = conf.getString("HOST");
  val httpProtocol = http
    // .baseUrl(url)
    .baseUrl("https://stg-api.zolve.com")
    .acceptHeader(" application/json")
    .acceptEncodingHeader("gzip, deflate")

  val userCount = Integer.getInteger("Users", 1).toInt
  val testDuration = Integer.getInteger("Duration", 10).toInt

  before {
    println(s"Running test with ${userCount} users")
    // println(s"Running test with ${testDuration} seconds")
    // println(s"Running test with host ${url}")
  }

  val rand = new Random()
  val Alphanumeric = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes

  def mkStr(chars: Array[Byte], length: Int): String = {
    val bytes = new Array[Byte](length)
    for (i <- 0 until length) bytes(i) = chars(rand.nextInt(chars.length))
    new String(bytes, StandardCharsets.US_ASCII)
  }
  def nextAlphanumeric(length: Int): String = mkStr(Alphanumeric, length)

  val randomEmail = nextAlphanumeric(5) + "@test.com"
  
  val scn = scenario("OnboardingSimulation with v3")
    .exec(http("create new user")
      .post("/api/v1/account/registration/create-temp-user/")
      .header("content-type", "application/json")
      .body(RawFileBody("onboarding_v3/common/create_user.json"))
      .check(jsonPath("$..temp_user_id").find.saveAs("temp_user_id"))
    )

    .exec(http("Enter email OTP")
      .post("/api/v1/account/registration/verify-otp/")
      .header("content-type", "application/json")
      .body(ElFileBody("onboarding_v3/common/email_OTP.json"))
      .check(jsonPath("$.code").is("200"))
    )

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
      .check(jsonPath("$..applicationId[0]").find.saveAs("application_Id"))
    )
    
    // .exec(http("Next api for task")
    //   .post("/api/v1/application/task/next/")
    //   .header("content-type", "application/json")
    //   .header("Authorization", "Bearer ${access_token}")
    //   .body(RawFileBody("onboarding_v3/azpire/next.json"))
    //   .check(jsonPath("$.code").is("200"))
    //   .check(jsonPath("$.message").is("Fetched next task."))
    //   .check(jsonPath("$..taskId").find.saveAs("task_Id"))
    // )
    
//   setUp(scn.inject(constantConcurrentUsers(userCount).during(testDuration))).protocols(httpProtocol)
  setUp(scn.inject(atOnceUsers(userCount))).protocols(httpProtocol)
}
