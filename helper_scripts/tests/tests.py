from unittest import TestCase
from ffm import format_digits


class TestFormatting(TestCase):

    def test_format_digits(self):
        tests = (
            ('2.333333', '2.33'),
            ('2', '2'),
            ('2.33', '2.33'))
        for test in tests:
            self.assertEqual(format_digits(test[0]), test[1])
