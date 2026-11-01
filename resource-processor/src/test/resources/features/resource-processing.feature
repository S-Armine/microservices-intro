Feature: Resource processing
  When a new MP3 has been uploaded, its id arrives on the processing queue. The processor
  downloads the file from the resource service, reads the tags and duration and hands the
  result to the song service.

  Scenario: A newly uploaded MP3 is turned into song metadata
    Given the resource service holds the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965" as resource 1
    And the song service accepts new metadata
    When the id "1" arrives on the processing queue
    Then the song service receives metadata for song 1
    And the metadata names the song "Yesterday" by "The Beatles" from the album "Help!"
    And the metadata has the year "1965" and the duration "00:03"

  Scenario: A file that is not an MP3 produces no metadata
    Given the resource service holds a file that is not an MP3 as resource 2
    When the id "2" arrives on the processing queue
    Then processing fails with the message "The request body is invalid MP3"
    And the song service receives nothing

  Scenario: A resource that no longer exists produces no metadata
    Given the resource service does not know resource 3
    When the id "3" arrives on the processing queue
    Then processing fails because the resource was not found
    And the song service receives nothing

  Scenario: A short outage of the resource service is retried
    Given the resource service fails once and then holds the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965" as resource 4
    And the song service accepts new metadata
    When the id "4" arrives on the processing queue
    Then the song service receives metadata for song 4

  Scenario: Metadata that was already stored is not stored twice
    Given the resource service holds the song "Yesterday" by "The Beatles" from the album "Help!" released in "1965" as resource 5
    And the song service already has metadata for song 5
    When the id "5" arrives on the processing queue
    Then processing fails because the song already exists
