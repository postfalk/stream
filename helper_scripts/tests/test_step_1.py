from unittest import TestCase
from step_1_ffm import format_digits


class TestFormatting(TestCase):

    def test_format_digits(self):
        tests = (
            ('2.333333', '2.33'),
            ('2', '2.00'),
            ('2.33', '2.33'),
            ('0.01234', '0.0123'),
            ('0.001236', '0.00124'),
            ('-0.001236', '-0.00124'),
            ('23.0', '23.0'),
            ('123', '123'),
            ('1234567', '1230000'),
            ('1', '1.00'),
            ('-1', '-1.00'),
            ('-1.0', '-1.00'),
            ('-1.00', '-1.00'))
        for test in tests:
            print(test)
            self.assertEqual(format_digits(test[0]), test[1])
