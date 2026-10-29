@e2e
Feature: Audio file lifecycle across the whole system
  These scenarios use only the public APIs. Nothing is mocked: the resource service, the
  resource processor, the song service, RabbitMQ, S3 and both databases are all running.

  Scenario: An uploaded MP3 becomes searchable song metadata
    Given an MP3 file of the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965"
    When the file is uploaded to the resource service
    Then the resource service answers with the id of the new resource
    And within 30 seconds the song service has metadata for that id
    And the metadata names the song "Yesterday" by "The Beatles" from the album "Help!"
    And the metadata has the year "1965" and the duration "00:03"

  Scenario: The stored file can be downloaded unchanged
    Given an MP3 file of the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965"
    And the file is uploaded to the resource service
    When the file is downloaded from the resource service
    Then the downloaded content equals the uploaded file

  Scenario: Deleting a resource removes the file and its song metadata
    Given an MP3 file of the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965"
    And the file is uploaded to the resource service
    And within 30 seconds the song service has metadata for that id
    When the resource is deleted
    Then the resource service lists the id as deleted
    And the resource service no longer finds the resource
    And the song service no longer has metadata for that id

  Scenario: Something that is not an MP3 is refused
    When a file of type "application/json" is uploaded to the resource service
    Then the resource service refuses it with status 400

  Scenario: A resource that was never uploaded cannot be downloaded
    When the resource with id 2147483000 is downloaded
    Then the resource service answers with status 404
